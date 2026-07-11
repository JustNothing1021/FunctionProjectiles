package com.justnothing.functionprojectiles;

import com.justnothing.functionprojectiles.block.ModScreenHandlers;
import com.justnothing.functionprojectiles.client.FunctionAnvilScreen;
import com.justnothing.functionprojectiles.client.TooltipHandler;
import com.justnothing.functionprojectiles.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.entity.Entity;

public class FunctionProjectilesModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModScreenHandlers.FUNCTION_ANVIL, FunctionAnvilScreen::new);
        TooltipHandler.register();

        // Receive trajectory sync packets and apply position + velocity directly
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.TrajectorySyncPayload.TYPE,
            (payload, context) -> {
                Minecraft mc = Minecraft.getInstance();
                mc.execute(() -> {
                    if (mc.level == null) return;
                    Entity entity = mc.level.getEntity(payload.entityId());
                    if (entity != null) {
                        entity.setPos(payload.x(), payload.y(), payload.z());
                        entity.setDeltaMovement(payload.vx(), payload.vy(), payload.vz());
                    }
                });
            }
        );
    }
}
