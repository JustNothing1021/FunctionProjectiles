package com.justnothing.functionprojectiles.item;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import com.justnothing.functionprojectiles.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {

    public static final CreativeModeTab FUNCTION_PROJECTILES = FabricCreativeModeTab.builder()
        .icon(() -> new ItemStack(ModBlocks.FUNCTION_ANVIL))
        .title(Component.translatable("itemGroup.function-projectiles"))
        .displayItems((parameters, output) -> {
            output.accept(ModBlocks.FUNCTION_ANVIL);
        })
        .build();

    public static void register() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(FunctionProjectilesMod.MOD_ID, "general"),
            FUNCTION_PROJECTILES);
    }
}
