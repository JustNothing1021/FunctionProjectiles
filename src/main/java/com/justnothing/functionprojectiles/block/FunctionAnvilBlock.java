package com.justnothing.functionprojectiles.block;

import net.minecraft.block.AnvilBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FunctionAnvilBlock extends AnvilBlock {

    public FunctionAnvilBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        player.openHandledScreen(createScreenHandlerFactory(state, pos));
        return ActionResult.CONSUME;
    }

    private NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory(
            (syncId, inventory, player) ->
                new FunctionAnvilScreenHandler(syncId, inventory,
                    ScreenHandlerContext.create(player.getWorld(), pos)),
            Text.translatable("container.function-projectiles.function_anvil")
        );
    }
}
