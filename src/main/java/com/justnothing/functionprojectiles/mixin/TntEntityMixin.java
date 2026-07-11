package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.network.ModNetworking;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryData;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(PrimedTnt.class)
public abstract class TntEntityMixin {

    private static final double MAX_PARTICLE_SPACING = 0.1;
    private static final double MAX_INTERPOLATION_DIST = 10.0;

    private static final ConcurrentHashMap<UUID, Integer> collisionHits = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> startedInBlock = new ConcurrentHashMap<>();
    /** Target position set at HEAD, applied at TAIL for accurate client sync. */
    private static final ConcurrentHashMap<UUID, Vec3> targetPositions = new ConcurrentHashMap<>();
    /** Trajectory velocity set at TAIL so client interpolates in the right direction. */
    private static final ConcurrentHashMap<UUID, Vec3> targetVelocities = new ConcurrentHashMap<>();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        if (FunctionTrajectory.hasTrajectory(self.getUUID())) {
            self.setFuse(80); // prevent auto-explosion from fuse
        }
        if (self.level().isClientSide()) return;

        UUID uuid = self.getUUID();
        if (!FunctionTrajectory.hasTrajectory(uuid)) return;

        TrajectoryData data = FunctionTrajectory.getTrajectory(uuid);
        if (data == null) return;

        // Check if we're starting inside a block (only on first trajectory tick)
        if (!startedInBlock.containsKey(uuid)) {
            BlockPos currentBlock = BlockPos.containing(self.position());
            VoxelShape shape = self.level().getBlockState(currentBlock)
                .getCollisionShape(self.level(), currentBlock);
            startedInBlock.put(uuid, !shape.isEmpty());
            collisionHits.put(uuid, 0);
        }

        TrajectoryResult result = FunctionTrajectory.computeNextPosition(data);

        switch (result.getType()) {
            case NAN -> {
                spawnNaNEffect(self);
                cleanup(uuid);
                self.discard();
            }
            case INFINITE_UP, INFINITE_DOWN -> {
                boolean up = result.getType() == TrajectoryResult.Type.INFINITE_UP;
                double stepSize = data.speed() / 20.0;
                Vec3 currentPos = self.position();
                double targetY = currentPos.y + (up ? stepSize : -stepSize);
                Vec3 targetPos = new Vec3(currentPos.x, targetY, currentPos.z);

                // Entity hit detection along path
                EntityHitResult entityHit = findEntityOnPath(self, currentPos, targetPos);
                if (entityHit != null) {
                    Vec3 hitPos = entityHit.getLocation();
                    cleanup(uuid);
                    self.level().explode(self, hitPos.x, hitPos.y, hitPos.z, 4.0f,
                        Level.ExplosionInteraction.TNT);
                    self.discard();
                    return;
                }

                // Block raycast along path
                Vec3 blockHitPos = findBlockOnPath(self, currentPos, targetPos);
                if (blockHitPos != null) {
                    boolean skip = startedInBlock.getOrDefault(uuid, false)
                        && currentPos.distanceTo(blockHitPos) < 0.5;
                    if (!skip) {
                        cleanup(uuid);
                        self.level().explode(self, blockHitPos.x, blockHitPos.y, blockHitPos.z, 4.0f,
                            Level.ExplosionInteraction.TNT);
                        self.discard();
                        return;
                    }
                }

                if (checkCollision(self, uuid, targetPos)) return;

                Vec3 targetVel = new Vec3(0, up ? stepSize : -stepSize, 0);
                targetPositions.put(uuid, targetPos);
                targetVelocities.put(uuid, targetVel);
                self.setPos(currentPos.x, targetY, currentPos.z);
                spawnParticles(self, currentPos, self.position());
                FunctionTrajectory.setTrajectory(uuid, new TrajectoryData(
                    data.mode(), data.expression(), data.parametric(),
                    data.origin(), data.forward(), data.up(), data.right(),
                    data.speed(), result.getNewParam()
                ));
            }
            case POSITION -> {
                Vec3 newPos = result.getPosition();
                Vec3 oldPos = self.position();

                // Entity hit detection along path
                EntityHitResult entityHit = findEntityOnPath(self, oldPos, newPos);
                if (entityHit != null) {
                    Vec3 hitPos = entityHit.getLocation();
                    cleanup(uuid);
                    self.level().explode(self, hitPos.x, hitPos.y, hitPos.z, 4.0f,
                        Level.ExplosionInteraction.TNT);
                    self.discard();
                    return;
                }

                // Block raycast along path (prevents tunneling through thin blocks)
                Vec3 blockHitPos = findBlockOnPath(self, oldPos, newPos);
                if (blockHitPos != null) {
                    boolean skip = startedInBlock.getOrDefault(uuid, false)
                        && oldPos.distanceTo(blockHitPos) < 0.5;
                    if (!skip) {
                        cleanup(uuid);
                        self.level().explode(self, blockHitPos.x, blockHitPos.y, blockHitPos.z, 4.0f,
                            Level.ExplosionInteraction.TNT);
                        self.discard();
                        return;
                    }
                }

                if (checkCollision(self, uuid, newPos)) return;

                Vec3 targetVel = newPos.subtract(oldPos);
                targetPositions.put(uuid, newPos);
                targetVelocities.put(uuid, targetVel);
                self.setPos(newPos.x, newPos.y, newPos.z);
                spawnParticles(self, oldPos, newPos);
                FunctionTrajectory.setTrajectory(uuid, new TrajectoryData(
                    data.mode(), data.expression(), data.parametric(),
                    data.origin(), data.forward(), data.up(), data.right(),
                    data.speed(), result.getNewParam()
                ));
            }
        }
    }

    /** Raycast along path to find block collisions (prevents tunneling through thin blocks). */
    private Vec3 findBlockOnPath(PrimedTnt self, Vec3 from, Vec3 to) {
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, self);
        BlockHitResult result = self.level().clip(context);
        if (result.getType() == HitResult.Type.BLOCK) {
            return result.getLocation();
        }
        return null;
    }

    /** Detects entities along the line segment from `from` to `to`, returning the closest hit. */
    private EntityHitResult findEntityOnPath(PrimedTnt self, Vec3 from, Vec3 to) {
        Vec3 step = to.subtract(from);
        AABB pathBox = self.getBoundingBox().expandTowards(step).inflate(1.0);
        List<Entity> entities = self.level().getEntities(self, pathBox);
        Entity closest = null;
        Vec3 closestHit = null;
        double closestDistSq = Double.MAX_VALUE;
        for (Entity entity : entities) {
            if (!entity.isAlive() || !entity.isPickable()) continue;
            if (entity == self.getOwner()) continue;
            // Inflate entity bounding box by 0.3 to account for TNT's size
            AABB hitBox = entity.getBoundingBox().inflate(0.3);
            Optional<Vec3> hit = hitBox.clip(from, to);
            if (hit.isPresent()) {
                double distSq = from.distanceToSqr(hit.get());
                if (distSq < closestDistSq) {
                    closestDistSq = distSq;
                    closest = entity;
                    closestHit = hit.get();
                }
            }
        }
        if (closest != null && closestHit != null) {
            return new EntityHitResult(closest, closestHit);
        }
        return null;
    }

    /** Returns true if the TNT should explode (collision threshold met). */
    private boolean checkCollision(PrimedTnt self, UUID uuid, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        VoxelShape shape = self.level().getBlockState(blockPos)
            .getCollisionShape(self.level(), blockPos);
        if (shape.isEmpty()) {
            // No collision this tick — reset hit counter
            collisionHits.put(uuid, 0);
            return false;
        }

        int hits = collisionHits.merge(uuid, 1, Integer::sum);
        boolean skipFirst = startedInBlock.getOrDefault(uuid, false);

        if (skipFirst && hits <= 1) return false; // skip the initial block contact

        // Explode!
        cleanup(uuid);
        self.level().explode(self, pos.x, pos.y, pos.z, 4.0f,
            Level.ExplosionInteraction.TNT);
        self.discard();
        return true;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        PrimedTnt self = (PrimedTnt) (Object) this;
        if (self.level().isClientSide()) return;
        UUID uuid = self.getUUID();
        if (!self.isAlive()) {
            cleanup(uuid);
            return;
        }
        if (FunctionTrajectory.hasTrajectory(uuid)) {
            // Apply exact position and trajectory velocity at tick end
            Vec3 targetPos = targetPositions.remove(uuid);
            Vec3 targetVel = targetVelocities.remove(uuid);
            if (targetPos != null) {
                self.setPos(targetPos.x, targetPos.y, targetPos.z);
            }
            if (targetVel != null) {
                self.setDeltaMovement(targetVel.x, targetVel.y, targetVel.z);
            }
            // Send custom sync packet every tick to bypass the entity
            // tracker's limited update interval (10 ticks for TNT!)
            if (targetPos != null && targetVel != null) {
                var packet = new ModNetworking.TrajectorySyncPayload(
                    self.getId(),
                    targetPos.x, targetPos.y, targetPos.z,
                    targetVel.x, targetVel.y, targetVel.z
                );
                for (ServerPlayer player : PlayerLookup.tracking(self)) {
                    ServerPlayNetworking.send(player, packet);
                }
            }
        }
    }

    private void spawnParticles(PrimedTnt self, Vec3 from, Vec3 to) {
        if (!(self.level() instanceof ServerLevel serverLevel)) return;
        double dist = to.distanceTo(from);
        if (dist < 0.005) return;

        if (dist > MAX_INTERPOLATION_DIST) {
            for (var player : PlayerLookup.tracking(self)) {
                serverLevel.sendParticles(player, ParticleTypes.END_ROD, true, false,
                    to.x, to.y, to.z, 1, 0, 0, 0, 0);
            }
            return;
        }

        int steps = Math.max(1, (int) Math.ceil(dist / MAX_PARTICLE_SPACING));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double px = from.x + (to.x - from.x) * t;
            double py = from.y + (to.y - from.y) * t;
            double pz = from.z + (to.z - from.z) * t;
            for (var player : PlayerLookup.tracking(self)) {
                serverLevel.sendParticles(player, ParticleTypes.END_ROD, true, false,
                    px, py, pz, 1, 0, 0, 0, 0);
            }
        }
    }

    private void spawnNaNEffect(PrimedTnt self) {
        if (self.level() instanceof ServerLevel serverLevel) {
            Vec3 pos = self.position();
            for (var player : PlayerLookup.tracking(self)) {
                serverLevel.sendParticles(player, ParticleTypes.EXPLOSION, true, false,
                    pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            }
            serverLevel.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }

    private static void cleanup(UUID uuid) {
        FunctionTrajectory.removeTrajectory(uuid);
        collisionHits.remove(uuid);
        startedInBlock.remove(uuid);
        targetPositions.remove(uuid);
        targetVelocities.remove(uuid);
    }
}
