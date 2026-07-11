package com.justnothing.functionprojectiles.client;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TooltipHandler implements ItemTooltipCallback {

    @Override
    public void getTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
        FunctionComponent funcComp = stack.get(ModComponents.FUNCTION);
        if (funcComp != null) {
            lines.add(Component.translatable(
                "function-projectiles.tooltip.expression",
                funcComp.expression()
            ));
            return;
        }

        ParametricComponent paramComp = stack.get(ModComponents.PARAMETRIC);
        if (paramComp != null) {
            lines.add(Component.translatable(
                "function-projectiles.tooltip.parametric",
                paramComp.expressionX(),
                paramComp.expressionY(),
                paramComp.expressionZ()
            ));
        }
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register(new TooltipHandler());
    }
}
