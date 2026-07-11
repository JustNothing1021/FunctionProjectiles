package com.justnothing.functionprojectiles;

import com.justnothing.functionprojectiles.block.FunctionAnvilScreenHandler;
import com.justnothing.functionprojectiles.block.ModBlocks;
import com.justnothing.functionprojectiles.block.ModScreenHandlers;
import com.justnothing.functionprojectiles.command.ModCommands;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.item.ModItemGroups;
import com.justnothing.functionprojectiles.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FunctionProjectilesMod implements ModInitializer {
    public static final String MOD_ID = "function-projectiles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModComponents.register();
        ModBlocks.register();
        ModScreenHandlers.register();
        ModItemGroups.register();
        ModNetworking.register();
        ModCommands.register();

        ServerPlayNetworking.registerGlobalReceiver(ModNetworking.FunctionAnvilUpdatePayload.TYPE,
            (payload, context) -> {
                if (context.player().containerMenu instanceof FunctionAnvilScreenHandler handler) {
                    handler.setMode(payload.mode());
                    handler.setNewItemName(payload.expression());
                }
            }
        );
        LOGGER.info("Function Projectiles loaded!");
    }
}
