package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.expression.ExprParseException;
import com.justnothing.functionprojectiles.expression.ExprParser;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory;
import com.justnothing.functionprojectiles.trajectory.TntHelper;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TntBlock.class)
public class TntBlockMixin {

    /** After the static prime spawns the entity, look up saved data and apply trajectory. */
    @Inject(method = "prime(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("TAIL"))
    private static void onPrime(Level level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        TntHelper.TntPlacement data = TntHelper.consume(pos);
        if (data == null) return;

        for (Entity entity : level.getEntities(null, new AABB(pos).inflate(1))) {
            if (entity instanceof PrimedTnt tnt) {
                applyTrajectory(tnt, data);
                return;
            }
        }
    }

    /** When a TNT block is destroyed by an explosion, apply trajectory to the spawned PrimedTnt. */
    @Inject(method = "wasExploded", at = @At("TAIL"))
    private void onWasExploded(ServerLevel level, BlockPos pos, Explosion explosion, CallbackInfo ci) {
        TntHelper.TntPlacement data = TntHelper.consume(pos);
        if (data == null) return;

        for (Entity entity : level.getEntities(null, new AABB(pos).inflate(1))) {
            if (entity instanceof PrimedTnt tnt) {
                applyTrajectory(tnt, data);
                return;
            }
        }
    }

    private static void applyTrajectory(PrimedTnt tnt, TntHelper.TntPlacement placement) {
        Vec3 origin = tnt.position();
        double yawRad = placement.yaw() * Math.PI / 180.0;
        Vec3 forward = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = forward.cross(up).normalize();
        up = right.cross(forward).normalize();

        double speed = 1.0;

        if (placement.function() != null) {
            try {
                var expr = ExprParser.parse(placement.function().expression());
                var td = new FunctionTrajectory.TrajectoryData(
                    FunctionTrajectory.Mode.FUNCTION, expr, null,
                    origin, forward, up, right, speed, 0);
                FunctionTrajectory.setTrajectory(tnt.getUUID(), td);
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
                FunctionTrajectory.setTrajectory(tnt.getUUID(), td);
            } catch (ExprParseException ignored) {}
        }
    }
}
