package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.expression.ExprParseException;
import com.justnothing.functionprojectiles.expression.ExprParser;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory;
import com.justnothing.functionprojectiles.trajectory.TntHelper;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntBlock.class)
public class TntBlockMixin {

    /** After the static primeTnt spawns the entity, look up saved data and apply trajectory. */
    @Inject(method = "primeTnt(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V",
            at = @At("TAIL"))
    private static void onPrimeTnt(World world, BlockPos pos, CallbackInfo ci) {
        TntHelper.TntPlacement data = TntHelper.consume(pos);
        if (data == null) return;

        for (Entity entity : world.getOtherEntities(null, new Box(pos).expand(1))) {
            if (entity instanceof TntEntity tnt) {
                applyTrajectory(tnt, data);
                return;
            }
        }
    }

    private static void applyTrajectory(TntEntity tnt, TntHelper.TntPlacement placement) {
        Vec3d origin = tnt.getPos();
        double yawRad = placement.yaw() * Math.PI / 180.0;
        Vec3d forward = new Vec3d(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3d up = new Vec3d(0, 1, 0);
        Vec3d right = forward.crossProduct(up).normalize();
        up = right.crossProduct(forward).normalize();

        double speed = 1.0;

        if (placement.function() != null) {
            try {
                var expr = ExprParser.parse(placement.function().expression());
                var td = new FunctionTrajectory.TrajectoryData(
                    FunctionTrajectory.Mode.FUNCTION, expr, null,
                    origin, forward, up, right, speed, 0);
                FunctionTrajectory.setTrajectory(tnt.getUuid(), td);
            } catch (ExprParseException ignored) {}
        } else if (placement.parametric() != null) {
            try {
                var ex = ExprParser.parseForT(placement.parametric().expressionX());
                var ey = ExprParser.parseForT(placement.parametric().expressionY());
                var ez = ExprParser.parseForT(placement.parametric().expressionZ());
                var parametric = new com.justnothing.functionprojectiles.expression.ParametricExpression(ex, ey, ez);
                var td = new FunctionTrajectory.TrajectoryData(
                    FunctionTrajectory.Mode.PARAMETRIC, null, parametric,
                    origin, forward, up, right, speed, 0);
                FunctionTrajectory.setTrajectory(tnt.getUuid(), td);
            } catch (ExprParseException ignored) {}
        }
    }
}
