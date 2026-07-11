package com.justnothing.functionprojectiles.trajectory;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.expression.ExprParseException;
import com.justnothing.functionprojectiles.expression.ExprParser;
import com.justnothing.functionprojectiles.expression.Expression;
import com.justnothing.functionprojectiles.expression.ParametricExpression;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FunctionTrajectory {

    /** Number of extra steps to look ahead when encountering NaN/Infinity before giving up. */
    private static final int LOOKAHEAD_STEPS = 3;
    /** Maximum absolute value for any world coordinate — prevents long overflow in chunk system. */
    private static final double MAX_COORD = 3.0E7;

    public enum Mode {
        FUNCTION,   // f(x) mode
        PARAMETRIC  // x(t), y(t), z(t) mode
    }

    public static record TrajectoryData(
        Mode mode,
        Expression expression,           // for f(x) mode
        ParametricExpression parametric, // for parametric mode
        Vec3 origin,                     // spawn position
        Vec3 forward,                    // player's horizontal view direction (normalized)
        Vec3 up,                         // MC y-axis (0,1,0)
        Vec3 right,                      // cross product forward x up
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
            FunctionComponent component, Projectile projectile) {
        return createFromFunction(component, projectile, projectile.getDeltaMovement().length());
    }

    public static TrajectoryData createFromFunction(
            FunctionComponent component, Projectile projectile, double speed) {
        try {
            Expression expr = ExprParser.parse(component.expression());
            Vec3 origin = getOwnerEyePos(projectile);
            CoordinateFrame frame = buildCoordinateFrame(projectile);
            return new TrajectoryData(Mode.FUNCTION, expr, null,
                origin, frame.forward(), frame.up(), frame.right(), speed, 0);
        } catch (ExprParseException e) {
            return null;
        }
    }

    public static TrajectoryData createFromParametric(
            ParametricComponent component, Projectile projectile) {
        return createFromParametric(component, projectile, projectile.getDeltaMovement().length());
    }

    public static TrajectoryData createFromParametric(
            ParametricComponent component, Projectile projectile, double speed) {
        try {
            Expression exprX = ExprParser.parseForT(component.expressionX());
            Expression exprY = ExprParser.parseForT(component.expressionY());
            Expression exprZ = ExprParser.parseForT(component.expressionZ());
            ParametricExpression parametric = new ParametricExpression(exprX, exprY, exprZ);
            Vec3 origin = getOwnerEyePos(projectile);
            CoordinateFrame frame = buildCoordinateFrame(projectile);
            return new TrajectoryData(Mode.PARAMETRIC, null, parametric,
                origin, frame.forward(), frame.up(), frame.right(), speed, 0);
        } catch (ExprParseException e) {
            return null;
        }
    }

    /** Use the owner's eye position as origin so the trajectory starts from the view point. */
    private static Vec3 getOwnerEyePos(Projectile projectile) {
        Entity owner = projectile.getOwner();
        if (owner != null) {
            return owner.getEyePosition();
        }
        return projectile.position();
    }

    /** Full 3D right-handed coordinate frame. */
    public record CoordinateFrame(Vec3 forward, Vec3 up, Vec3 right) {}

    /**
     * Builds a right-handed 3D coordinate frame from the projectile's velocity.
     * x = velocity direction (full 3D, not projected to horizontal)
     * y = velocity rotated 90° CCW in the vertical plane (spanned by velocity and world-up)
     * z = x × y
     */
    private static CoordinateFrame buildCoordinateFrame(Projectile projectile) {
        Vec3 forward;
        Entity owner = projectile.getOwner();

        if (owner != null) {
            // Pure throw direction = projectile velocity minus owner's movement
            Vec3 throwDir = projectile.getDeltaMovement().subtract(owner.getDeltaMovement());
            if (throwDir.lengthSqr() > 1e-6) {
                forward = throwDir.normalize();
            } else {
                float y = owner.getYRot(), p = owner.getXRot();
                double yr = y * Math.PI / 180, pr = p * Math.PI / 180;
                forward = new Vec3(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
            }
        } else {
            Vec3 vel = projectile.getDeltaMovement();
            if (vel.lengthSqr() > 1e-6) {
                forward = vel.normalize();
            } else {
                forward = new Vec3(0, 0, 1);
            }
        }

        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 n = forward.cross(worldUp);

        Vec3 up;
        if (n.lengthSqr() < 1e-6) {
            float yaw = owner != null ? owner.getYRot() : 0;
            double r = yaw * Math.PI / 180;
            Vec3 href = new Vec3(-Math.sin(r), 0, Math.cos(r));
            up = forward.cross(href.cross(forward)).normalize();
        } else {
            up = n.cross(forward).normalize();
        }

        return new CoordinateFrame(forward, up, forward.cross(up));
    }

    /**
     * Compute the next position for a projectile with a function trajectory.
     * Returns null if the expression evaluates to NaN (destroy projectile),
     * or a Vec3 with a special marker for Infinity (vertical movement).
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

            if (isInvalid(y) || isInvalid(prevY)) {
                return tryLookaheadFunction(data, newParam, dt, isInvalid(y) ? y : prevY);
            }

            // Subdivide if y changes too fast (prevents skipping over the ground)
            double dy = Math.abs(y - prevY);
            if (dy > MAX_Y_CHANGE_PER_TICK && dt > 0.01) {
                double scale = MAX_Y_CHANGE_PER_TICK / dy;
                newParam = data.currentParam() + dt * scale;
                y = data.expression().evaluate(newParam);
                if (isInvalid(y)) {
                    return tryLookaheadFunction(data, newParam, dt, y);
                }
            }

            Vec3 localPos = new Vec3(newParam, y, 0);
            Vec3 worldPos = localToWorld(localPos, data);
            if (!isInBounds(worldPos)) return TrajectoryResult.nan();
            return TrajectoryResult.position(worldPos, newParam);

        } else {
            ParametricExpression.Point point = data.parametric().evaluate(newParam);

            if (isInvalid(point.x()) || isInvalid(point.y()) || isInvalid(point.z())) {
                return tryLookaheadParametric(data, newParam, dt, point);
            }

            Vec3 localPos = new Vec3(point.x(), point.y(), point.z());
            Vec3 worldPos = localToWorld(localPos, data);
            if (!isInBounds(worldPos)) return TrajectoryResult.nan();
            return TrajectoryResult.position(worldPos, newParam);
        }
    }

    private static boolean isInvalid(double value) {
        return Double.isNaN(value) || Double.isInfinite(value);
    }

    private static boolean isInBounds(Vec3 pos) {
        return Math.abs(pos.x) <= MAX_COORD
            && Math.abs(pos.y) <= MAX_COORD
            && Math.abs(pos.z) <= MAX_COORD;
    }

    /** Look ahead a few steps to see if the function recovers from NaN/Infinity. */
    private static TrajectoryResult tryLookaheadFunction(TrajectoryData data,
            double currentParam, double dt, double firstValue) {
        for (int i = 1; i <= LOOKAHEAD_STEPS; i++) {
            double lookParam = currentParam + dt * i;
            double y = data.expression().evaluate(lookParam);
            if (!isInvalid(y)) {
                Vec3 localPos = new Vec3(lookParam, y, 0);
                Vec3 worldPos = localToWorld(localPos, data);
                if (isInBounds(worldPos)) return TrajectoryResult.position(worldPos, lookParam);
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
                Vec3 localPos = new Vec3(point.x(), point.y(), point.z());
                Vec3 worldPos = localToWorld(localPos, data);
                if (isInBounds(worldPos)) return TrajectoryResult.position(worldPos, lookParam);
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
    public static Vec3 localToWorld(Vec3 local, TrajectoryData data) {
        return data.origin().add(
            data.forward().scale(local.x)
                .add(data.up().scale(local.y))
                .add(data.right().scale(local.z))
        );
    }

    public static class TrajectoryResult {
        public enum Type { POSITION, NAN, INFINITE_UP, INFINITE_DOWN }

        private final Type type;
        private final Vec3 position;
        private final double newParam;

        private TrajectoryResult(Type type, Vec3 position, double newParam) {
            this.type = type;
            this.position = position;
            this.newParam = newParam;
        }

        public static TrajectoryResult position(Vec3 pos, double newParam) {
            return new TrajectoryResult(Type.POSITION, pos, newParam);
        }

        public static TrajectoryResult nan() {
            return new TrajectoryResult(Type.NAN, null, 0);
        }

        public static TrajectoryResult infinite(boolean positive) {
            return new TrajectoryResult(positive ? Type.INFINITE_UP : Type.INFINITE_DOWN, null, 0);
        }

        public Type getType() { return type; }
        public Vec3 getPosition() { return position; }
        public double getNewParam() { return newParam; }
    }
}
