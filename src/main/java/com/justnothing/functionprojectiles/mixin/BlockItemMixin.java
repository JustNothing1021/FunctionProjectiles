package com.justnothing.functionprojectiles.mixin;

import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.trajectory.TntHelper;
import net.minecraft.block.TntBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

    @Inject(method = "place", at = @At("RETURN"))
    private void onPlace(ItemPlacementContext context, CallbackInfoReturnable<ActionResult> cir) {
        if (!cir.getReturnValue().isAccepted()) return;
        ItemStack stack = context.getStack();

        if (!(stack.getItem() instanceof BlockItem bi)) return;
        if (!(bi.getBlock() instanceof TntBlock)) return;

        var funcComp = stack.get(ModComponents.FUNCTION);
        var paramComp = stack.get(ModComponents.PARAMETRIC);
        if (funcComp == null && paramComp == null) return;

        PlayerEntity player = context.getPlayer();
        float yaw = player != null ? player.getYaw() : 0;
        TntHelper.save(context.getBlockPos(), funcComp, paramComp, yaw);
    }
}
