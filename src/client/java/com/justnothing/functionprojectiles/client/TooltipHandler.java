package com.justnothing.functionprojectiles.client;

import com.justnothing.functionprojectiles.component.FunctionComponent;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.component.ParametricComponent;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.util.List;

public class TooltipHandler implements ItemTooltipCallback {

    @Override
    public void getTooltip(ItemStack stack, TooltipContext context, List<Text> lines) {
        FunctionComponent funcComp = ModComponents.getFunction(stack);
        if (funcComp != null) {
            lines.add(Text.translatable(
                "function-projectiles.tooltip.expression",
                funcComp.expression()
            ));
            return;
        }

        ParametricComponent paramComp = ModComponents.getParametric(stack);
        if (paramComp != null) {
            lines.add(Text.translatable(
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
