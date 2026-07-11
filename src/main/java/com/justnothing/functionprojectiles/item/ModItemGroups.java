package com.justnothing.functionprojectiles.item;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import com.justnothing.functionprojectiles.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {

    public static final ItemGroup FUNCTION_PROJECTILES = FabricItemGroup.builder()
        .icon(() -> new ItemStack(ModBlocks.FUNCTION_ANVIL))
        .displayName(Text.translatable("itemGroup.function-projectiles"))
        .entries((displayContext, entries) -> {
            entries.add(ModBlocks.FUNCTION_ANVIL);
        })
        .build();

    public static void register() {
        Registry.register(Registries.ITEM_GROUP,
            Identifier.of(FunctionProjectilesMod.MOD_ID, "general"),
            FUNCTION_PROJECTILES);
    }
}
