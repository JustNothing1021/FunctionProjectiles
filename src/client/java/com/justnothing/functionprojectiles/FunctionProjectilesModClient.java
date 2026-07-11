package com.justnothing.functionprojectiles;

import com.justnothing.functionprojectiles.block.ModScreenHandlers;
import com.justnothing.functionprojectiles.client.FunctionAnvilScreen;
import com.justnothing.functionprojectiles.client.TooltipHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class FunctionProjectilesModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlers.FUNCTION_ANVIL, FunctionAnvilScreen::new);
        TooltipHandler.register();
    }
}
