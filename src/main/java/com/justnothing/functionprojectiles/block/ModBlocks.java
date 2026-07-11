package com.justnothing.functionprojectiles.block;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

    private static final Identifier FUNCTION_ANVIL_ID =
        Identifier.fromNamespaceAndPath(FunctionProjectilesMod.MOD_ID, "function_anvil");
    private static final ResourceKey<Block> FUNCTION_ANVIL_BLOCK_KEY =
        ResourceKey.create(Registries.BLOCK, FUNCTION_ANVIL_ID);
    private static final ResourceKey<Item> FUNCTION_ANVIL_ITEM_KEY =
        ResourceKey.create(Registries.ITEM, FUNCTION_ANVIL_ID);

    public static final FunctionAnvilBlock FUNCTION_ANVIL = new FunctionAnvilBlock(
        BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL)
            .setId(FUNCTION_ANVIL_BLOCK_KEY)
    );

    public static void register() {
        Registry.register(BuiltInRegistries.BLOCK, FUNCTION_ANVIL_ID, FUNCTION_ANVIL);
        Registry.register(BuiltInRegistries.ITEM, FUNCTION_ANVIL_ID,
            new BlockItem(FUNCTION_ANVIL, new Item.Properties().setId(FUNCTION_ANVIL_ITEM_KEY)));
    }
}
