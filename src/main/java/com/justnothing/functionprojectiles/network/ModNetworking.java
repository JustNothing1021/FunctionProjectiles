package com.justnothing.functionprojectiles.network;

import com.justnothing.functionprojectiles.FunctionProjectilesMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public class ModNetworking {

    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(
            FunctionAnvilUpdatePayload.TYPE,
            FunctionAnvilUpdatePayload.CODEC
        );
        PayloadTypeRegistry.clientboundPlay().register(
            TrajectorySyncPayload.TYPE,
            TrajectorySyncPayload.CODEC
        );
    }

    public record FunctionAnvilUpdatePayload(
        String expression,
        String mode
    ) implements CustomPacketPayload {

        public static final Identifier PAYLOAD_ID =
            Identifier.fromNamespaceAndPath(FunctionProjectilesMod.MOD_ID, "function_anvil_update");

        public static final CustomPacketPayload.Type<FunctionAnvilUpdatePayload> TYPE =
            new CustomPacketPayload.Type<>(PAYLOAD_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, FunctionAnvilUpdatePayload> CODEC =
            StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, FunctionAnvilUpdatePayload::expression,
                ByteBufCodecs.STRING_UTF8, FunctionAnvilUpdatePayload::mode,
                FunctionAnvilUpdatePayload::new
            );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /**
     * Server-to-client packet that syncs precise trajectory entity position &amp; velocity
     * every tick, bypassing the vanilla entity tracker's limited update interval.
     */
    public record TrajectorySyncPayload(
        int entityId,
        double x, double y, double z,
        double vx, double vy, double vz
    ) implements CustomPacketPayload {

        public static final Identifier PAYLOAD_ID =
            Identifier.fromNamespaceAndPath(FunctionProjectilesMod.MOD_ID, "trajectory_sync");

        public static final CustomPacketPayload.Type<TrajectorySyncPayload> TYPE =
            new CustomPacketPayload.Type<>(PAYLOAD_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, TrajectorySyncPayload> CODEC =
            StreamCodec.composite(
                ByteBufCodecs.INT, TrajectorySyncPayload::entityId,
                ByteBufCodecs.DOUBLE, TrajectorySyncPayload::x,
                ByteBufCodecs.DOUBLE, TrajectorySyncPayload::y,
                ByteBufCodecs.DOUBLE, TrajectorySyncPayload::z,
                ByteBufCodecs.DOUBLE, TrajectorySyncPayload::vx,
                ByteBufCodecs.DOUBLE, TrajectorySyncPayload::vy,
                ByteBufCodecs.DOUBLE, TrajectorySyncPayload::vz,
                TrajectorySyncPayload::new
            );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
