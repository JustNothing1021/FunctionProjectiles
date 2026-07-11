package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.trajectory.TntHelper;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!cir.getReturnValue().consumesAction()) return;
        ItemStack stack = context.getItemInHand();

        if (!(stack.getItem() instanceof BlockItem bi)) return;
        if (!(bi.getBlock() instanceof TntBlock)) return;

        var funcComp = stack.get(ModComponents.FUNCTION);
        var paramComp = stack.get(ModComponents.PARAMETRIC);
        if (funcComp == null && paramComp == null) return;

        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() : 0;
        TntHelper.save(context.getClickedPos(), funcComp, paramComp, yaw);
    }
}
