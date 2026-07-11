package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryData;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryResult;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(TntEntity.class)
public abstract class TntEntityMixin {

    private static final double MAX_PARTICLE_SPACING = 0.1;
    private static final double MAX_INTERPOLATION_DIST = 10.0;

    private static final ConcurrentHashMap<UUID, Integer> collisionHits = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Boolean> startedInBlock = new ConcurrentHashMap<>();

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        TntEntity self = (TntEntity) (Object) this;
        if (FunctionTrajectory.hasTrajectory(self.getUuid())) {
            self.setFuse(80); // prevent auto-explosion from fuse
        }
        if (self.getWorld().isClient) return;

        UUID uuid = self.getUuid();
        if (!FunctionTrajectory.hasTrajectory(uuid)) return;

        TrajectoryData data = FunctionTrajectory.getTrajectory(uuid);
        if (data == null) return;

        // Check if we're starting inside a block (only on first trajectory tick)
        if (!startedInBlock.containsKey(uuid)) {
            BlockPos currentBlock = BlockPos.ofFloored(self.getPos());
            VoxelShape shape = self.getWorld().getBlockState(currentBlock)
                .getCollisionShape(self.getWorld(), currentBlock);
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
                Vec3d currentPos = self.getPos();
                double targetY = currentPos.y + (up ? stepSize : -stepSize);

                if (checkCollision(self, uuid, new Vec3d(currentPos.x, targetY, currentPos.z))) return;

                self.setPos(currentPos.x, targetY, currentPos.z);
                spawnParticles(self, currentPos, self.getPos());
                FunctionTrajectory.setTrajectory(uuid, new TrajectoryData(
                    data.mode(), data.expression(), data.parametric(),
                    data.origin(), data.forward(), data.up(), data.right(),
                    data.speed(), result.getNewParam()
                ));
            }
            case POSITION -> {
                Vec3d newPos = result.getPosition();

                if (checkCollision(self, uuid, newPos)) return;

                Vec3d oldPos = self.getPos();
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

    /** Returns true if the TNT should explode (collision threshold met). */
    private boolean checkCollision(TntEntity self, UUID uuid, Vec3d pos) {
        BlockPos blockPos = BlockPos.ofFloored(pos);
        VoxelShape shape = self.getWorld().getBlockState(blockPos)
            .getCollisionShape(self.getWorld(), blockPos);
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
        self.getWorld().createExplosion(self, pos.x, pos.y, pos.z, 4.0f,
            World.ExplosionSourceType.TNT);
        self.discard();
        return true;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTickTail(CallbackInfo ci) {
        TntEntity self = (TntEntity) (Object) this;
        if (self.getWorld().isClient) return;
        if (!self.isAlive()) {
            cleanup(self.getUuid());
        }
    }

    private void spawnParticles(TntEntity self, Vec3d from, Vec3d to) {
        if (!(self.getWorld() instanceof ServerWorld serverWorld)) return;
        double dist = to.distanceTo(from);
        if (dist < 0.005) return;
        if (dist > MAX_INTERPOLATION_DIST) {
            serverWorld.spawnParticles(ParticleTypes.END_ROD, to.x, to.y, to.z,
                1, 0, 0, 0, 0);
            return;
        }
        int steps = Math.max(1, (int) Math.ceil(dist / MAX_PARTICLE_SPACING));
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            serverWorld.spawnParticles(ParticleTypes.END_ROD,
                from.x + (to.x - from.x) * t,
                from.y + (to.y - from.y) * t,
                from.z + (to.z - from.z) * t,
                1, 0, 0, 0, 0);
        }
    }

    private void spawnNaNEffect(TntEntity self) {
        if (self.getWorld() instanceof ServerWorld serverWorld) {
            Vec3d pos = self.getPos();
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
                pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
            serverWorld.playSound(null, pos.x, pos.y, pos.z,
                SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0f, 1.0f);
        }
    }

    private static void cleanup(UUID uuid) {
        FunctionTrajectory.removeTrajectory(uuid);
        collisionHits.remove(uuid);
        startedInBlock.remove(uuid);
    }
}
