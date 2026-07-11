package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryData;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryResult;
import com.justnothing.functionprojectiles.trajectory.TrajectoryHelper;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin {

    private static final double MAX_PARTICLE_SPACING = 0.05;
    private static final double MIN_THROW_SPEED = 0.3;

    private static final ConcurrentHashMap<UUID, Double> originalSpeeds = new ConcurrentHashMap<>();
    /** Pre-computed particle positions per tick, cleared after spawning. */
    private static final ConcurrentHashMap<UUID, List<Vec3d>> pendingParticles = new ConcurrentHashMap<>();
    /** Entities whose trajectory has been permanently released (e.g., trident loyalty). */
    private static final Set<UUID> released = new HashSet<>();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        if (self.getWorld().isClient) return;
        UUID uuid = self.getUuid();

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
                if (self.getWorld() instanceof ServerWorld sw) {
                    Vec3d p = self.getPos();
                    sw.spawnParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 1, 0, 0, 0, 0);
                    sw.playSound(null, p.x, p.y, p.z, SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1, 1);
                }
                cleanup(uuid);
                self.discard();
            }
            case INFINITE_UP, INFINITE_DOWN -> {
                boolean up = result.getType() == TrajectoryResult.Type.INFINITE_UP;
                double step = data.speed() * 0.6 / 20.0;
                Vec3d cp = self.getPos();
                double ty = cp.y + (up ? step : -step);
                if (ty > self.getWorld().getTopY() || ty < self.getWorld().getBottomY()
                    || !self.getWorld().getBlockState(BlockPos.ofFloored(cp.x, ty, cp.z))
                        .getCollisionShape(self.getWorld(), BlockPos.ofFloored(cp.x, ty, cp.z)).isEmpty()) {
                    cleanup(uuid);
                    self.discard();
                    return;
                }
                self.setVelocity(0, up ? step : -step, 0);
                FunctionTrajectory.setTrajectory(uuid, new TrajectoryData(
                    data.mode(), data.expression(), data.parametric(), data.origin(),
                    data.forward(), data.up(), data.right(), data.speed(), result.getNewParam()));
                pendingParticles.put(uuid, List.of(self.getPos()));
            }
            case POSITION -> {
                Vec3d np = result.getPosition();
                Vec3d vel = np.subtract(self.getPos());
                BlockPos bp = BlockPos.ofFloored(np);
                boolean hit = !self.getWorld().getBlockState(bp).getCollisionShape(self.getWorld(), bp).isEmpty();
                self.setVelocity(vel.x, vel.y, vel.z);
                if (hit) { cleanup(uuid); return; }
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
    }

    private static boolean shouldSkipTrajectory(ProjectileEntity self) {
        if (self instanceof TridentEntity trident) {
            if (trident.getOwner() != null && trident.getVelocity().length() < 0.5) {
                released.add(self.getUuid());
                return true;
            }
        }
        if (self instanceof FishingBobberEntity) {
            if (self.isTouchingWater()) {
                released.add(self.getUuid());
                return true;
            }
        }
        return false;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        ProjectileEntity self = (ProjectileEntity) (Object) this;
        if (self.getWorld().isClient) return;
        UUID uuid = self.getUuid();
        if (!self.isAlive()) { cleanup(uuid); return; }
        if (FunctionTrajectory.hasTrajectory(uuid)) {
            ((EntityFieldsAccessor)(Object)this).setVelocityDirty(true);
            spawnPendingParticles(self, uuid);
        }
    }

    private void spawnPendingParticles(ProjectileEntity self, UUID uuid) {
        List<Vec3d> positions = pendingParticles.remove(uuid);
        if (positions == null || !(self.getWorld() instanceof ServerWorld sw)) return;
        for (Vec3d pos : positions) {
            for (var player : sw.getPlayers()) {
                sw.spawnParticles(player, ParticleTypes.END_ROD, true, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private void computeParticles(TrajectoryData data, UUID uuid, double newParam) {
        double oldParam = data.currentParam();
        double span = Math.abs(newParam - oldParam);
        int count = Math.max(1, (int)Math.ceil(span / MAX_PARTICLE_SPACING));

        List<Vec3d> positions = new java.util.ArrayList<>();
        if (data.mode() == FunctionTrajectory.Mode.FUNCTION) {
            for (int i = 1; i <= count; i++) {
                double p = oldParam + (newParam - oldParam) * i / count;
                double y = data.expression().evaluate(p);
                positions.add(FunctionTrajectory.localToWorld(new Vec3d(p, y, 0), data));
            }
        } else {
            for (int i = 1; i <= count; i++) {
                double p = oldParam + (newParam - oldParam) * i / count;
                var pt = data.parametric().evaluate(p);
                positions.add(FunctionTrajectory.localToWorld(new Vec3d(pt.x(), pt.y(), pt.z()), data));
            }
        }
        pendingParticles.put(uuid, positions);
    }

    private void tryApplyFromItem(ProjectileEntity self) {
        if (self instanceof ThrownItemEntity ti) tryApplyThrown(ti);
        else if (self instanceof PersistentProjectileEntity ar) tryApplyArrow(ar);
        else if (self instanceof FishingBobberEntity fb) tryApplyBobber(fb);
        else if (self instanceof FireworkRocketEntity fr) tryApplyFirework(fr);
    }

    private void tryApplyThrown(ThrownItemEntity ti) {
        try {
            ItemStack stack = ti.getDataTracker().get(ThrownItemEntityAccessor.getItemTrackedData());
            applyIfPresent(ti, stack);
        } catch (Exception ignored) {}
    }

    private void tryApplyArrow(PersistentProjectileEntity arrow) {
        if (applyIfPresent(arrow, arrow.getItemStack())) return;
        if (arrow.getOwner() instanceof PlayerEntity p) {
            for (ItemStack hs : new ItemStack[]{p.getMainHandStack(), p.getOffHandStack()})
                if ((hs.getItem() instanceof BowItem || hs.getItem() instanceof CrossbowItem) && applyIfPresent(arrow, hs)) return;
        }
    }

    private void tryApplyBobber(FishingBobberEntity bobber) {
        if (bobber.getOwner() instanceof PlayerEntity p) {
            for (ItemStack hs : new ItemStack[]{p.getMainHandStack(), p.getOffHandStack()})
                if (hs.getItem() instanceof FishingRodItem && applyIfPresent(bobber, hs)) return;
        }
    }

    private void tryApplyFirework(FireworkRocketEntity rocket) {
        try {
            ItemStack stack = rocket.getDataTracker().get(FireworkRocketEntityAccessor.getItemTrackedData());
            if (applyIfPresent(rocket, stack)) return;
        } catch (Exception ignored) {}
        if (rocket.getOwner() instanceof PlayerEntity p) {
            for (ItemStack hs : new ItemStack[]{p.getMainHandStack(), p.getOffHandStack()})
                if (hs.getItem() instanceof CrossbowItem && applyIfPresent(rocket, hs)) return;
        }
    }

    private boolean applyIfPresent(ProjectileEntity self, ItemStack stack) {
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

    private static double resolveSpeed(ProjectileEntity self) {
        UUID uuid = self.getUuid();
        double speed = self.getVelocity().length();
        if (speed >= MIN_THROW_SPEED) { originalSpeeds.put(uuid, speed); return speed; }
        return originalSpeeds.getOrDefault(uuid, 1.0);
    }
}
