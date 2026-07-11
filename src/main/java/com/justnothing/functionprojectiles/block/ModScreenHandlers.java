package com.justnothing.functionprojectiles.block;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlers {

    public static final ScreenHandlerType<FunctionAnvilScreenHandler> FUNCTION_ANVIL =
        new ScreenHandlerType<>(FunctionAnvilScreenHandler::new, FeatureFlags.VANILLA_FEATURES);

    public static void register() {
        Registry.register(Registries.SCREEN_HANDLER,
            new Identifier(FunctionProjectilesMod.MOD_ID, "function_anvil"),
            FUNCTION_ANVIL);
    }
}
