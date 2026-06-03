package com.combatlog;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public final class CombatLogNetworking {

    public record TagTimerPayload(long remainingMs) implements CustomPacketPayload {

        public static final Identifier TAG_TIMER_ID =
                Identifier.fromNamespaceAndPath(CombatLogMod.MOD_ID, "tag_timer");

        public static final CustomPacketPayload.Type<TagTimerPayload> TYPE =
                new CustomPacketPayload.Type<>(TAG_TIMER_ID);

        public static final StreamCodec<RegistryFriendlyByteBuf, TagTimerPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.VAR_LONG,
                        TagTimerPayload::remainingMs,
                        TagTimerPayload::new
                );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private CombatLogNetworking() {}

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TagTimerPayload.TYPE, TagTimerPayload.CODEC);
    }

    public static void sendTagTime(ServerPlayer player, long remainingMs) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new TagTimerPayload(remainingMs));
    }
}