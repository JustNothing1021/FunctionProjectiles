package com.justnothing.functionprojectiles;

import com.justnothing.functionprojectiles.block.ModBlocks;
import com.justnothing.functionprojectiles.block.FunctionAnvilScreenHandler;
import com.justnothing.functionprojectiles.block.ModScreenHandlers;
import com.justnothing.functionprojectiles.component.ModComponents;
import com.justnothing.functionprojectiles.network.ModNetworking;
import com.justnothing.functionprojectiles.item.ModItemGroups;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
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

        ServerPlayNetworking.registerGlobalReceiver(ModNetworking.FUNCTION_ANVIL_UPDATE,
            (server, player, handler, buf, responseSender) -> {
                String mode = buf.readString();
                String expression = buf.readString();
                server.execute(() -> {
                    if (player.currentScreenHandler instanceof FunctionAnvilScreenHandler fh) {
                        fh.setMode(mode);
                        fh.setNewItemName(expression);
                    }
                });
            }
        );

        ServerPlayNetworking.registerGlobalReceiver(ModNetworking.FUNCTION_ANVIL_CRAFT,
            (server, player, handler, buf, responseSender) -> {
                String expr = buf.readString();
                String mode = buf.readString();
                server.execute(() -> {
                    if (player.currentScreenHandler instanceof FunctionAnvilScreenHandler fh) {
                        ItemStack input = fh.getInputStack();
                        if (input.isEmpty()) return;
                        ItemStack result = input.copy();
                        result.setCount(1);
                        if ("parametric".equals(mode)) {
                            String[] parts = expr.split("\\|", 3);
                            if (parts.length == 3)
                                ModComponents.setParametric(result, parts[0].trim(), parts[1].trim(), parts[2].trim());
                        } else {
                            ModComponents.setFunction(result, expr.trim());
                        }
                        input.setCount(input.getCount() - 1);
                        if (input.isEmpty()) input.setCount(0); // force empty
                        player.getInventory().offerOrDrop(result);
                        fh.sendContentUpdates();
                    }
                });
            }
        );
        LOGGER.info("Function Projectiles loaded!");
    }
}
