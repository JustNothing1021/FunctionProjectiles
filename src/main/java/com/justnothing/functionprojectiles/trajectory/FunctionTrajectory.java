package com.justnothing.functionprojectiles.trajectory;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.expression.ExprParseException;
import com.justnothing.functionprojectiles.expression.ExprParser;
import com.justnothing.functionprojectiles.expression.Expression;
import com.justnothing.functionprojectiles.expression.ParametricExpression;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.Vec3d;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionTrajectory {

    /** Number of extra steps to look ahead when encountering NaN/Infinity before giving up. */
    private static final int LOOKAHEAD_STEPS = 3;

    public enum Mode {
        FUNCTION,   // f(x) mode
        PARAMETRIC  // x(t), y(t), z(t) mode
    }

    public static record TrajectoryData(
        Mode mode,
        Expression expression,           // for f(x) mode
        ParametricExpression parametric, // for parametric mode
        Vec3d origin,                    // spawn position
        Vec3d forward,                   // player's horizontal view direction (normalized)
        Vec3d up,                        // MC y-axis (0,1,0)
        Vec3d right,                     // cross product forward x up
        double speed,                    // projectile speed
        double currentParam              // current x or t value
    ) {}

    private static final ConcurrentHashMap<UUID, TrajectoryData> trajectories = new ConcurrentHashMap<>();

    public static void setTrajectory(UUID entityId, TrajectoryData data) {
        trajectories.put(entityId, data);
    }

    public static TrajectoryData getTrajectory(UUID entityId) {
        return trajectories.get(entityId);
    }

    public static void removeTrajectory(UUID entityId) {
        trajectories.remove(entityId);
    }

    public static boolean hasTrajectory(UUID entityId) {
        return trajectories.containsKey(entityId);
    }

    /**
     * Initialize trajectory from a FunctionComponent on an item stack.
     * Call this when a projectile is spawned.
     */
    public static TrajectoryData createFromFunction(
            FunctionComponent component, ProjectileEntity projectile) {
        return createFromFunction(component, projectile, projectile.getVelocity().length());
    }

    public static TrajectoryData createFromFunction(
            FunctionComponent component, ProjectileEntity projectile, double speed) {
        try {
            Expression expr = ExprParser.parse(component.expression());
            Vec3d origin = projectile.getPos();
            CoordinateFrame frame = buildCoordinateFrame(projectile);
            return new TrajectoryData(Mode.FUNCTION, expr, null,
                origin, frame.forward(), frame.up(), frame.right(), speed, 0);
        } catch (ExprParseException e) {
            return null;
        }
    }

    public static TrajectoryData createFromParametric(
            ParametricComponent component, ProjectileEntity projectile) {
        return createFromParametric(component, projectile, projectile.getVelocity().length());
    }

    public static TrajectoryData createFromParametric(
            ParametricComponent component, ProjectileEntity projectile, double speed) {
        try {
            Expression exprX = ExprParser.parseForT(component.expressionX());
            Expression exprY = ExprParser.parseForT(component.expressionY());
            Expression exprZ = ExprParser.parseForT(component.expressionZ());
            ParametricExpression parametric = new ParametricExpression(exprX, exprY, exprZ);
            Vec3d origin = projectile.getPos();
            CoordinateFrame frame = buildCoordinateFrame(projectile);
            return new TrajectoryData(Mode.PARAMETRIC, null, parametric,
                origin, frame.forward(), frame.up(), frame.right(), speed, 0);
        } catch (ExprParseException e) {
            return null;
        }
    }

    /** Full 3D right-handed coordinate frame. */
    public record CoordinateFrame(Vec3d forward, Vec3d up, Vec3d right) {}

    /**
     * Builds a right-handed 3D coordinate frame from the projectile's velocity.
     * x = velocity direction (full 3D, not projected to horizontal)
     * y = velocity rotated 90° CCW in the vertical plane (spanned by velocity and world-up)
     * z = x × y
     */
    private static CoordinateFrame buildCoordinateFrame(ProjectileEntity projectile) {
        Vec3d forward;
        Entity owner = projectile.getOwner();

        if (owner != null) {
            // Pure throw direction = projectile velocity minus owner's movement
            Vec3d throwDir = projectile.getVelocity().subtract(owner.getVelocity());
            if (throwDir.lengthSquared() > 1e-6) {
                forward = throwDir.normalize();
            } else {
                float y = owner.getYaw(), p = owner.getPitch();
                double yr = y * Math.PI / 180, pr = p * Math.PI / 180;
                forward = new Vec3d(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
            }
        } else {
            Vec3d vel = projectile.getVelocity();
            if (vel.lengthSquared() > 1e-6) {
                forward = vel.normalize();
            } else {
                forward = new Vec3d(0, 0, 1);
            }
        }

        Vec3d worldUp = new Vec3d(0, 1, 0);
        Vec3d n = forward.crossProduct(worldUp);

        Vec3d up;
        if (n.lengthSquared() < 1e-6) {
            float yaw = owner != null ? owner.getYaw() : 0;
            double r = yaw * Math.PI / 180;
            Vec3d href = new Vec3d(-Math.sin(r), 0, Math.cos(r));
            up = forward.crossProduct(href.crossProduct(forward)).normalize();
        } else {
            up = n.crossProduct(forward).normalize();
        }

        return new CoordinateFrame(forward, up, forward.crossProduct(up));
    }

    /**
     * Compute the next position for a projectile with a function trajectory.
     * Returns null if the expression evaluates to NaN (destroy projectile),
     * or a Vec3d with a special marker for Infinity (vertical movement).
     */
    private static final double MAX_SPEED_MULTIPLIER = 10.0;
    private static final double MAX_Y_CHANGE_PER_TICK = 4.0;
    private static final double SPEED_FACTOR = 0.6; // x-axis speed = 0.6 × throw speed

    public static TrajectoryResult computeNextPosition(TrajectoryData data) {
        double dt = data.speed() * SPEED_FACTOR;
        double newParam = data.currentParam() + dt;

        if (data.mode() == Mode.FUNCTION) {
            double prevY = data.expression().evaluate(data.currentParam());
            double y = data.expression().evaluate(newParam);

            if (isInvalid(y)) {
                return tryLookaheadFunction(data, newParam, dt, y);
            }

            // Subdivide if y changes too fast (prevents skipping over the ground)
            double dy = Math.abs(y - prevY);
            if (dy > MAX_Y_CHANGE_PER_TICK && dt > 0.01) {
                double scale = MAX_Y_CHANGE_PER_TICK / dy;
                newParam = data.currentParam() + dt * scale;
                y = data.expression().evaluate(newParam);
            }

            Vec3d localPos = new Vec3d(newParam, y, 0);
            return TrajectoryResult.position(localToWorld(localPos, data), newParam);

        } else {
            ParametricExpression.Point point = data.parametric().evaluate(newParam);

            if (isInvalid(point.x()) || isInvalid(point.y()) || isInvalid(point.z())) {
                return tryLookaheadParametric(data, newParam, dt, point);
            }

            Vec3d localPos = new Vec3d(point.x(), point.y(), point.z());
            return TrajectoryResult.position(localToWorld(localPos, data), newParam);
        }
    }

    private static boolean isInvalid(double value) {
        return Double.isNaN(value) || Double.isInfinite(value);
    }

    /** Look ahead a few steps to see if the function recovers from NaN/Infinity. */
    private static TrajectoryResult tryLookaheadFunction(TrajectoryData data,
            double currentParam, double dt, double firstValue) {
        for (int i = 1; i <= LOOKAHEAD_STEPS; i++) {
            double lookParam = currentParam + dt * i;
            double y = data.expression().evaluate(lookParam);
            if (!isInvalid(y)) {
                Vec3d localPos = new Vec3d(lookParam, y, 0);
                Vec3d worldPos = localToWorld(localPos, data);
                return TrajectoryResult.position(worldPos, lookParam);
            }
        }
        // All lookahead failed
        if (Double.isNaN(firstValue)) return TrajectoryResult.nan();
        return TrajectoryResult.infinite(firstValue > 0);
    }

    private static TrajectoryResult tryLookaheadParametric(TrajectoryData data,
            double currentParam, double dt, ParametricExpression.Point firstPoint) {
        for (int i = 1; i <= LOOKAHEAD_STEPS; i++) {
            double lookParam = currentParam + dt * i;
            ParametricExpression.Point point = data.parametric().evaluate(lookParam);
            if (!isInvalid(point.x()) && !isInvalid(point.y()) && !isInvalid(point.z())) {
                Vec3d localPos = new Vec3d(point.x(), point.y(), point.z());
                Vec3d worldPos = localToWorld(localPos, data);
                return TrajectoryResult.position(worldPos, lookParam);
            }
        }
        // All lookahead failed — check which component was NaN
        if (Double.isNaN(firstPoint.x()) || Double.isNaN(firstPoint.y()) || Double.isNaN(firstPoint.z())) {
            return TrajectoryResult.nan();
        }
        // All were Infinity — determine direction from y component
        return TrajectoryResult.infinite(firstPoint.y() > 0);
    }

    /**
     * Transform local coordinates to world coordinates.
     * Local: x = forward direction, y = up direction, z = right direction
     */
    public static Vec3d localToWorld(Vec3d local, TrajectoryData data) {
        return data.origin().add(
            data.forward().multiply(local.x)
                .add(data.up().multiply(local.y))
                .add(data.right().multiply(local.z))
        );
    }

    public static class TrajectoryResult {
        public enum Type { POSITION, NAN, INFINITE_UP, INFINITE_DOWN }

        private final Type type;
        private final Vec3d position;
        private final double newParam;

        private TrajectoryResult(Type type, Vec3d position, double newParam) {
            this.type = type;
            this.position = position;
            this.newParam = newParam;
        }

        public static TrajectoryResult position(Vec3d pos, double newParam) {
            return new TrajectoryResult(Type.POSITION, pos, newParam);
        }

        public static TrajectoryResult nan() {
            return new TrajectoryResult(Type.NAN, null, 0);
        }

        public static TrajectoryResult infinite(boolean positive) {
            return new TrajectoryResult(positive ? Type.INFINITE_UP : Type.INFINITE_DOWN, null, 0);
        }

        public Type getType() { return type; }
        public Vec3d getPosition() { return position; }
        public double getNewParam() { return newParam; }
    }
}
