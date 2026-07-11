package com.justnothing.functionprojectiles.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class ModNetworking {

    public static final Identifier FUNCTION_ANVIL_UPDATE = new Identifier("function-projectiles", "anvil_update");
    public static final Identifier FUNCTION_ANVIL_CRAFT = new Identifier("function-projectiles", "anvil_craft");

    public static void register() {
    }

    public static void sendAnvilUpdate(String mode, String expression) {
        // Called from client screen - just sent via ClientPlayNetworking in the client handler
    }

    public static String readMode(PacketByteBuf buf) {
        return buf.readString();
    }

    public static String readExpression(PacketByteBuf buf) {
        return buf.readString();
    }

    public static PacketByteBuf createAnvilPacket(String mode, String expression) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(mode);
        buf.writeString(expression);
        return buf;
    }
}
