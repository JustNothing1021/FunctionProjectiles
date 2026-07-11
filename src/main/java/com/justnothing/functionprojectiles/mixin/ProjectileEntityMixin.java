package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.network.ModNetworking;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryData;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryResult;
import com.justnothing.functionprojectiles.trajectory.TrajectoryHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WindChargeItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(Projectile.class)
public abstract class ProjectileEntityMixin {

    private static final double MAX_PARTICLE_SPACING = 0.05;
    private static final double MIN_THROW_SPEED = 0.3;

    private static final ConcurrentHashMap<UUID, Double> originalSpeeds = new ConcurrentHashMap<>();
    /** Pre-computed particle positions per tick, cleared after spawning. */
    private static final ConcurrentHashMap<UUID, List<Vec3>> pendingParticles = new ConcurrentHashMap<>();
    /** Entities whose trajectory has been permanently released (e.g., trident loyalty). */
    private static final Set<UUID> released = new HashSet<>();
    /** Target position set at HEAD, applied at TAIL for accurate client sync. */
    private static final ConcurrentHashMap<UUID, Vec3> targetPositions = new ConcurrentHashMap<>();
    /** Trajectory velocity set at TAIL so client interpolates in the right direction. */
    private static final ConcurrentHashMap<UUID, Vec3> targetVelocities = new ConcurrentHashMap<>();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (self.level().isClientSide()) return;
        UUID uuid = self.getUUID();

        if (!FunctionTrajectory.hasTrajectory(uuid) && !released.contains(uuid)) tryApplyFromItem(self);
        if (!FunctionTrajectory.hasTrajectory(uuid)) return;
        if (released.contains(uuid)) return;

        // Disable trajectory for loyalty-returning tridents and bobbers in water
        if (shouldSkipTrajectory(self)) {
            FunctionTrajectory.removeTrajectory(uuid);
            originalSpeeds.remove(uuid);
            return;
        }

        TrajectoryData data = FunctionTrajectory.getTrajectory(uuid);
        if (data == null) return;
        TrajectoryResult result = FunctionTrajectory.computeNextPosition(data);

        switch (result.getType()) {
            case NAN -> {
                if (self.level() instanceof ServerLevel sw) {
                    Vec3 p = self.position();
                    sw.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    sw.playSound(null, p.x, p.y, p.z, SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1, 1);
                }
                cleanup(uuid);
                self.discard();
            }
            case INFINITE_UP, INFINITE_DOWN -> {
                boolean up = result.getType() == TrajectoryResult.Type.INFINITE_UP;
                double step = data.speed() * 0.6 / 20.0;
                Vec3 cp = self.position();
                double ty = cp.y + (up ? step : -step);
                if (ty > self.level().getMaxY() + 1 || ty < self.level().getMinY()
                    || !self.level().getBlockState(BlockPos.containing(cp.x, ty, cp.z))
                        .getCollisionShape(self.level(), BlockPos.containing(cp.x, ty, cp.z)).isEmpty()) {
                    cleanup(uuid);
                    self.discard();
                    return;
                }
                Vec3 targetPos = new Vec3(cp.x, ty, cp.z);
                Vec3 targetVel = new Vec3(0, up ? step : -step, 0);
                targetPositions.put(uuid, targetPos);
                targetVelocities.put(uuid, targetVel);
                self.setDeltaMovement(targetVel.x, targetVel.y, targetVel.z);
                FunctionTrajectory.setTrajectory(uuid, new TrajectoryData(
                    data.mode(), data.expression(), data.parametric(), data.origin(),
                    data.forward(), data.up(), data.right(), data.speed(), result.getNewParam()));
                pendingParticles.put(uuid, List.of(self.position()));
            }
            case POSITION -> {
                Vec3 np = result.getPosition();
                Vec3 vel = np.subtract(self.position());
                BlockPos bp = BlockPos.containing(np);
                boolean hitBlock = !self.level().getBlockState(bp).getCollisionShape(self.level(), bp).isEmpty();
                targetPositions.put(uuid, np);
                targetVelocities.put(uuid, vel);
                self.setDeltaMovement(vel.x, vel.y, vel.z);

                // If vanilla's stepMoveAndHit already handled a hit (inGround=true), respect it
                if (self instanceof AbstractArrow arrow && ((AbstractArrowAccessor) arrow).callIsInGround()) {
                    cleanup(uuid);
                    return;
                }

                if (hitBlock) { cleanup(uuid); return; }
                FunctionTrajectory.setTrajectory(uuid, new TrajectoryData(
                    data.mode(), data.expression(), data.parametric(), data.origin(),
                    data.forward(), data.up(), data.right(), data.speed(), result.getNewParam()));
                computeParticles(data, uuid, result.getNewParam());
            }
        }
    }

    private static void cleanup(UUID uuid) {
        FunctionTrajectory.removeTrajectory(uuid);
        originalSpeeds.remove(uuid);
        pendingParticles.remove(uuid);
        released.remove(uuid);
        targetPositions.remove(uuid);
        targetVelocities.remove(uuid);
    }

    private static boolean shouldSkipTrajectory(Projectile self) {
        if (self instanceof ThrownTrident trident) {
            if (trident.getOwner() != null && trident.getDeltaMovement().length() < 0.5) {
                released.add(self.getUUID());
                return true;
            }
        }
        // Note: isInGround() is no longer checked here. When a trajectory is active,
        // vanilla collision may set inGround=true erroneously. AbstractArrowMixin
        // forces inGround=false at HEAD of AbstractArrow.tick() to prevent this
        // from skipping the flight branch. Landing is handled by our own collision
        // detection in the POSITION case.
        if (self instanceof FishingHook) {
            if (self.isInWater()) {
                released.add(self.getUUID());
                return true;
            }
        }
        return false;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        if (self.level().isClientSide()) return;
        UUID uuid = self.getUUID();
        if (!self.isAlive()) { cleanup(uuid); return; }
        if (FunctionTrajectory.hasTrajectory(uuid)) {
            // If vanilla handled an entity/block hit (inGround=true), don't override
            if (self instanceof AbstractArrow arrow && ((AbstractArrowAccessor) arrow).callIsInGround()) {
                cleanup(uuid);
                return;
            }
            // Apply exact position and trajectory velocity at tick end
            // so client receives correct data for interpolation
            Vec3 targetPos = targetPositions.remove(uuid);
            Vec3 targetVel = targetVelocities.remove(uuid);
            if (targetPos != null) {
                self.setPos(targetPos.x, targetPos.y, targetPos.z);
            }
            if (targetVel != null) {
                self.setDeltaMovement(targetVel.x, targetVel.y, targetVel.z);
            }
            // Force inGround=false: vanilla collision may have set it erroneously
            // (e.g., "inside block at current position" check). Only do this when
            // we're still controlling the trajectory (vanilla hit was NOT processed).
            if (self instanceof AbstractArrow arrow) {
                ((AbstractArrowAccessor) arrow).callSetInGround(false);
            }
            // Send custom sync packet every tick to bypass the entity
            // tracker's limited update interval (3 ticks for arrows, 10 for TNT)
            if (targetPos != null && targetVel != null && self.level() instanceof ServerLevel serverLevel) {
                var packet = new ModNetworking.TrajectorySyncPayload(
                    self.getId(),
                    targetPos.x, targetPos.y, targetPos.z,
                    targetVel.x, targetVel.y, targetVel.z
                );
                for (ServerPlayer player : PlayerLookup.tracking(self)) {
                    ServerPlayNetworking.send(player, packet);
                }
            }
            spawnPendingParticles(self, uuid);
        }
    }

    private void spawnPendingParticles(Projectile self, UUID uuid) {
        List<Vec3> positions = pendingParticles.remove(uuid);
        if (positions == null || !(self.level() instanceof ServerLevel sw)) return;
        for (Vec3 pos : positions) {
            for (var player : sw.players()) {
                sw.sendParticles(player, ParticleTypes.END_ROD, true, false, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void computeParticles(TrajectoryData data, UUID uuid, double newParam) {
        double oldParam = data.currentParam();
        double span = Math.abs(newParam - oldParam);
        int count = Math.max(1, (int)Math.ceil(span / MAX_PARTICLE_SPACING));

        List<Vec3> positions = new java.util.ArrayList<>();
        if (data.mode() == FunctionTrajectory.Mode.FUNCTION) {
            for (int i = 1; i <= count; i++) {
                double p = oldParam + (newParam - oldParam) * i / count;
                double y = data.expression().evaluate(p);
                positions.add(FunctionTrajectory.localToWorld(new Vec3(p, y, 0), data));
            }
        } else {
            for (int i = 1; i <= count; i++) {
                double p = oldParam + (newParam - oldParam) * i / count;
                var pt = data.parametric().evaluate(p);
                positions.add(FunctionTrajectory.localToWorld(new Vec3(pt.x(), pt.y(), pt.z()), data));
            }
        }
        pendingParticles.put(uuid, positions);
    }

    private void tryApplyFromItem(Projectile self) {
        if (self instanceof ThrowableItemProjectile ti) tryApplyThrownItem(ti);
        else if (self instanceof ThrowableProjectile ti) tryApplyThrown(ti);
        else if (self instanceof AbstractArrow ar) tryApplyArrow(ar);
        else if (self instanceof FishingHook fb) tryApplyBobber(fb);
        else if (self instanceof FireworkRocketEntity fr) tryApplyFirework(fr);
        else if (self instanceof WindCharge wc) tryApplyWindCharge(wc);
    }

    private void tryApplyThrownItem(ThrowableItemProjectile ti) {
        ItemStack stack = ti.getItem();
        applyIfPresent(ti, stack);
    }

    private void tryApplyThrown(ThrowableProjectile ti) {
        // Non-item throwables (like experience bottles in some mods) - skip
    }

    private void tryApplyArrow(AbstractArrow arrow) {
        if (applyIfPresent(arrow, arrow.getPickupItemStackOrigin())) return;
        if (arrow.getOwner() instanceof net.minecraft.world.entity.player.Player p) {
            for (ItemStack hs : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()})
                if ((hs.getItem() instanceof BowItem || hs.getItem() instanceof CrossbowItem) && applyIfPresent(arrow, hs)) return;
        }
    }

    private void tryApplyBobber(FishingHook bobber) {
        if (bobber.getOwner() instanceof net.minecraft.world.entity.player.Player p) {
            for (ItemStack hs : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()})
                if (hs.getItem() instanceof FishingRodItem && applyIfPresent(bobber, hs)) return;
        }
    }

    private void tryApplyFirework(FireworkRocketEntity rocket) {
        ItemStack stack = rocket.getItem();
        if (applyIfPresent(rocket, stack)) return;
        if (rocket.getOwner() instanceof net.minecraft.world.entity.player.Player p) {
            for (ItemStack hs : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()})
                if (hs.getItem() instanceof CrossbowItem && applyIfPresent(rocket, hs)) return;
        }
    }

    private void tryApplyWindCharge(WindCharge wc) {
        if (wc.getOwner() instanceof net.minecraft.world.entity.player.Player p) {
            for (ItemStack hs : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()})
                if (hs.getItem() instanceof WindChargeItem && applyIfPresent(wc, hs)) return;
        }
    }

    private boolean applyIfPresent(Projectile self, ItemStack stack) {
        FunctionComponent fc = stack.get(ModComponents.FUNCTION);
        if (fc != null) {
            double speed = resolveSpeed(self);
            FunctionProjectilesMod.LOGGER.info("Trajectory applied: expr={}, speed={}", fc.expression(), String.format("%.2f", speed));
            TrajectoryHelper.applyFunction(self, fc, speed);
            return true;
        }
        ParametricComponent pc = stack.get(ModComponents.PARAMETRIC);
        if (pc != null) {
            double speed = resolveSpeed(self);
            FunctionProjectilesMod.LOGGER.info("Parametric applied: speed={}", String.format("%.2f", speed));
            TrajectoryHelper.applyParametric(self, pc, speed);
            return true;
        }
        return false;
    }

    private static double resolveSpeed(Projectile self) {
        UUID uuid = self.getUUID();
        double speed = self.getDeltaMovement().length();
        if (speed >= MIN_THROW_SPEED) { originalSpeeds.put(uuid, speed); return speed; }
        return originalSpeeds.getOrDefault(uuid, 1.0);
    }
}
