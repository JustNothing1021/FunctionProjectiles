package com.justnothing.functionprojectiles.network;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public class ModNetworking {

    public static final CustomPayload.Id<FunctionAnvilUpdatePayload> FUNCTION_ANVIL_UPDATE =
        new CustomPayload.Id<>(Identifier.of(FunctionProjectilesMod.MOD_ID, "function_anvil_update"));

    public static void register() {
        PayloadTypeRegistry.playC2S().register(
            FunctionAnvilUpdatePayload.ID,
            FunctionAnvilUpdatePayload.CODEC
        );
    }

    public record FunctionAnvilUpdatePayload(
        String expression,
        String mode
    ) implements CustomPayload {

        public static final CustomPayload.Id<FunctionAnvilUpdatePayload> ID = FUNCTION_ANVIL_UPDATE;

        public static final PacketCodec<RegistryByteBuf, FunctionAnvilUpdatePayload> CODEC =
            PacketCodec.tuple(
                PacketCodecs.STRING, FunctionAnvilUpdatePayload::expression,
                PacketCodecs.STRING, FunctionAnvilUpdatePayload::mode,
                FunctionAnvilUpdatePayload::new
            );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }
}
