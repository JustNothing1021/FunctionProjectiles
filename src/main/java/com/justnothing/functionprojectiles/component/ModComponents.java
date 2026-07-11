package com.justnothing.functionprojectiles.component;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;

public class ModComponents {
    public static final DataComponentType<FunctionComponent> FUNCTION =
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(FunctionProjectilesMod.MOD_ID, "function"),
            DataComponentType.<FunctionComponent>builder()
                .persistent(FunctionComponent.CODEC)
                .networkSynchronized(ByteBufCodecs.fromCodec(FunctionComponent.CODEC))
                .build()
        );

    public static final DataComponentType<ParametricComponent> PARAMETRIC =
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(FunctionProjectilesMod.MOD_ID, "parametric"),
            DataComponentType.<ParametricComponent>builder()
                .persistent(ParametricComponent.CODEC)
                .networkSynchronized(ByteBufCodecs.fromCodec(ParametricComponent.CODEC))
                .build()
        );

    public static void register() {
        // Registration happens via class loading
    }
}
