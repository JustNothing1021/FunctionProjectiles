package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryData;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryResult;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Prevents vanilla gravity/inertia from modifying velocity when a trajectory is active.
 * ThrowableProjectile.tick() applies gravity and inertia BEFORE collision detection,
 * which causes the raycast to use the wrong velocity and produce false hits.
 */
@Mixin(ThrowableProjectile.class)
public abstract class ThrowableProjectileMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        ThrowableProjectile self = (ThrowableProjectile) (Object) this;
        if (self.level().isClientSide()) return;
        UUID uuid = self.getUUID();
        if (!FunctionTrajectory.hasTrajectory(uuid)) return;

        // Set trajectory velocity before gravity/inertia modify it
        TrajectoryData data = FunctionTrajectory.getTrajectory(uuid);
        if (data == null) return;
        TrajectoryResult result = FunctionTrajectory.computeNextPosition(data);
        if (result.getType() == TrajectoryResult.Type.POSITION) {
            Vec3 np = result.getPosition();
            Vec3 vel = np.subtract(self.position());
            self.setDeltaMovement(vel.x, vel.y, vel.z);
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;applyGravity()V"))
    private void skipGravity(ThrowableProjectile instance) {
        if (!instance.level().isClientSide() && FunctionTrajectory.hasTrajectory(instance.getUUID())) {
            return; // Trajectory controls velocity, skip gravity
        }
        // applyGravity() is protected in Entity; call via accessor
        ((GravityAccessor) instance).callApplyGravity();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrowableProjectile;applyInertia()V"))
    private void skipInertia(ThrowableProjectile instance) {
        if (!instance.level().isClientSide() && FunctionTrajectory.hasTrajectory(instance.getUUID())) {
            return; // Trajectory controls velocity, skip inertia
        }
        // applyInertia() is private in ThrowableProjectile; call via accessor
        ((InertiaAccessor) instance).callApplyInertia();
    }
}
