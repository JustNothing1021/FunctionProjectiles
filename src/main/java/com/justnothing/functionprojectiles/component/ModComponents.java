package com.justnothing.functionprojectiles.component;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModComponents {
    public static final ComponentType<FunctionComponent> FUNCTION =
        Registry.register(Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FunctionProjectilesMod.MOD_ID, "function"),
            ComponentType.<FunctionComponent>builder()
                .codec(FunctionComponent.CODEC)
                .build()
        );

    public static final ComponentType<ParametricComponent> PARAMETRIC =
        Registry.register(Registries.DATA_COMPONENT_TYPE,
            Identifier.of(FunctionProjectilesMod.MOD_ID, "parametric"),
            ComponentType.<ParametricComponent>builder()
                .codec(ParametricComponent.CODEC)
                .build()
        );

    public static void register() {
        // Registration happens via class loading
    }
}
