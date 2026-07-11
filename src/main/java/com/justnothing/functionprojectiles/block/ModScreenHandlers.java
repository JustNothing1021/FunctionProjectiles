package com.justnothing.functionprojectiles.block;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class ModScreenHandlers {

    public static final MenuType<FunctionAnvilScreenHandler> FUNCTION_ANVIL =
        new MenuType<>(FunctionAnvilScreenHandler::new, FeatureFlags.VANILLA_SET);

    public static void register() {
        Registry.register(BuiltInRegistries.MENU,
            Identifier.fromNamespaceAndPath(FunctionProjectilesMod.MOD_ID, "function_anvil"),
            FUNCTION_ANVIL);
    }
}
