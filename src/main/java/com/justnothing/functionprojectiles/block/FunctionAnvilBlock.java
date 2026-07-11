package com.justnothing.functionprojectiles.block;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class FunctionAnvilBlock extends AnvilBlock {

    public FunctionAnvilBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                    Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(createMenuProvider(state, pos));
        return InteractionResult.CONSUME;
    }

    private MenuProvider createMenuProvider(BlockState state, BlockPos pos) {
        return new SimpleMenuProvider(
            (id, inventory, player) ->
                new FunctionAnvilScreenHandler(id, inventory,
                    ContainerLevelAccess.create(player.level(), pos)),
            Component.translatable("container.function-projectiles.function_anvil")
        );
    }
}
