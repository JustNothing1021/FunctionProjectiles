package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryData;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryResult;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

/**
 * Prevents vanilla AbstractArrow collision detection from interfering with
 * trajectory-controlled arrows. When a trajectory is active:
 * - Force inGround=false so the flight branch always runs (and super.tick() is called)
 * - Set the trajectory velocity so AbstractArrow's collision raycast uses correct values
 * - Redirect isInGround() to return false so the "skip flight branch" check is bypassed
 */
@Mixin(AbstractArrow.class)
public class AbstractArrowMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTickHead(CallbackInfo ci) {
        AbstractArrow self = (AbstractArrow) (Object) this;
        if (self.level().isClientSide()) return;
        UUID uuid = self.getUUID();
        if (!FunctionTrajectory.hasTrajectory(uuid)) return;

        // Force inGround=false: vanilla may have set it erroneously, and if it stays true,
        // AbstractArrow.tick() skips the flight branch entirely (including super.tick()),
        // which means our ProjectileEntityMixin never runs.
        ((AbstractArrowAccessor) self).callSetInGround(false);

        // Pre-set the trajectory velocity so AbstractArrow.tick() reads it
        // for its collision raycast, instead of using stale velocity from the previous tick.
        TrajectoryData data = FunctionTrajectory.getTrajectory(uuid);
        if (data == null) return;
        TrajectoryResult result = FunctionTrajectory.computeNextPosition(data);
        if (result.getType() == TrajectoryResult.Type.POSITION) {
            Vec3 np = result.getPosition();
            Vec3 vel = np.subtract(self.position());
            self.setDeltaMovement(vel.x, vel.y, vel.z);
        }
    }

    /**
     * Redirect the isInGround() call in tick() that checks whether to skip the flight branch.
     * When a trajectory is active, always return false so the flight branch runs,
     * even if the "inside block at current position" check set inGround=true.
     */
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;isInGround()Z", ordinal = 0))
    private boolean redirectIsInGroundForFlightBranch(AbstractArrow instance) {
        if (FunctionTrajectory.hasTrajectory(instance.getUUID())) {
            return false;
        }
        return ((AbstractArrowAccessor) instance).callIsInGround();
    }
}
