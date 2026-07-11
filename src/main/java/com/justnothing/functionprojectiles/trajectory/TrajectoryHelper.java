package com.justnothing.functionprojectiles.trajectory;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import com.justnothing.functionprojectiles.trajectory.FunctionTrajectory.TrajectoryData;
import net.minecraft.entity.projectile.ProjectileEntity;

public class TrajectoryHelper {

    /**
     * Apply a function trajectory to a projectile.
     * Call this right after the projectile is spawned.
     * @return true if the trajectory was successfully applied
     */
    public static boolean applyFunction(ProjectileEntity projectile, FunctionComponent component, double speed) {
        TrajectoryData data = FunctionTrajectory.createFromFunction(component, projectile, speed);
        if (data != null) {
            FunctionTrajectory.setTrajectory(projectile.getUuid(), data);
            return true;
        }
        return false;
    }

    public static boolean applyParametric(ProjectileEntity projectile, ParametricComponent component, double speed) {
        TrajectoryData data = FunctionTrajectory.createFromParametric(component, projectile, speed);
        if (data != null) {
            FunctionTrajectory.setTrajectory(projectile.getUuid(), data);
            return true;
        }
        return false;
    }

    /**
     * Remove trajectory from a projectile.
     */
    public static void removeTrajectory(ProjectileEntity projectile) {
        FunctionTrajectory.removeTrajectory(projectile.getUuid());
    }

    /**
     * Check if a projectile has a function trajectory.
     */
    public static boolean hasTrajectory(ProjectileEntity projectile) {
        return FunctionTrajectory.hasTrajectory(projectile.getUuid());
    }
}
