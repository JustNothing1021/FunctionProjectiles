package com.justnothing.functionprojectiles.block;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModBlocks {

    public static final FunctionAnvilBlock FUNCTION_ANVIL = new FunctionAnvilBlock(
        Block.Settings.copy(Blocks.ANVIL)
    );

    public static void register() {
        registerBlock("function_anvil", FUNCTION_ANVIL);
    }

    private static void registerBlock(String name, Block block) {
        Identifier id = Identifier.of(FunctionProjectilesMod.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id,
            new BlockItem(block, new Item.Settings()));
    }
}
