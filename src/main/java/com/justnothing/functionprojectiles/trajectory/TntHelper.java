package com.justnothing.functionprojectiles.trajectory;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import net.minecraft.core.BlockPos;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores TNT placement data (component + direction) keyed by block position.
 * Written when TNT is placed, read when TNT is ignited.
 */
public class TntHelper {

    public record TntPlacement(
        FunctionComponent function,
        ParametricComponent parametric,
        float yaw
    ) {}

    private static final ConcurrentHashMap<BlockPos, TntPlacement> placements = new ConcurrentHashMap<>();

    public static void save(BlockPos pos, FunctionComponent func, ParametricComponent param, float yaw) {
        placements.put(pos.immutable(), new TntPlacement(func, param, yaw));
    }

    public static TntPlacement consume(BlockPos pos) {
        return placements.remove(pos.immutable());
    }

    public static void clearAll() {
        placements.clear();
    }
}
