package com.justnothing.functionprojectiles.trajectory;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryData;
import net.minecraft.world.entity.projectile.Projectile;

public class TrajectoryHelper {

    /**
     * Apply a function trajectory to a projectile.
     * Call this right after the projectile is spawned.
     * @return true if the trajectory was successfully applied
     */
    public static boolean applyFunction(Projectile projectile, FunctionComponent component, double speed) {
        TrajectoryData data = FunctionTrajectory.createFromFunction(component, projectile, speed);
        if (data != null) {
            FunctionTrajectory.setTrajectory(projectile.getUUID(), data);
            return true;
        }
        return false;
    }

    public static boolean applyParametric(Projectile projectile, ParametricComponent component, double speed) {
        TrajectoryData data = FunctionTrajectory.createFromParametric(component, projectile, speed);
        if (data != null) {
            FunctionTrajectory.setTrajectory(projectile.getUUID(), data);
            return true;
        }
        return false;
    }

    /**
     * Remove trajectory from a projectile.
     */
    public static void removeTrajectory(Projectile projectile) {
        FunctionTrajectory.removeTrajectory(projectile.getUUID());
    }

    /**
     * Check if a projectile has a function trajectory.
     */
    public static boolean hasTrajectory(Projectile projectile) {
        return FunctionTrajectory.hasTrajectory(projectile.getUUID());
    }
}
