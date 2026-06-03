package com.combatlog;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.StreamDecoder;
import net.minecraft.network.codec.StreamEncoder;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class CombatLogNetworking {

    // 1. Define the payload as a record
    public record TagTimerPayload(long remainingMs) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<TagTimerPayload> TYPE =
                new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(CombatLogMod.MOD_ID, "tag_timer")
                );

        public static final StreamCodec<FriendlyByteBuf, TagTimerPayload> CODEC =
                StreamCodec.of(
                        (StreamEncoder<FriendlyByteBuf, TagTimerPayload>) (buf, payload) ->
                                buf.writeLong(payload.remainingMs()),
                        (StreamDecoder<FriendlyByteBuf, TagTimerPayload>) buf ->
                                new TagTimerPayload(buf.readLong())
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private CombatLogNetworking() { }

    // 2. Call this once during mod init to register the payload type
    public static void register() {
        PayloadTypeRegistry.playS2C().register(TagTimerPayload.TYPE, TagTimerPayload.CODEC);
    }

    // 3. Send the packet
    public static void sendTagTime(ServerPlayer player, long remainingMs) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new TagTimerPayload(remainingMs));
    }
}