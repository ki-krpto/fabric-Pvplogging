package com.combatlog;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class CombatLogNetworking {

    public record TagTimerPayload(long remainingMs) implements CustomPayload {

        public static final Identifier TAG_TIMER_ID =
                Identifier.of(CombatLogMod.MOD_ID, "tag_timer");

        public static final CustomPayload.Id<TagTimerPayload> TYPE =
                new CustomPayload.Id<>(TAG_TIMER_ID);

        public static final PacketCodec<RegistryByteBuf, TagTimerPayload> CODEC =
                PacketCodec.tuple(
                        PacketCodecs.VAR_LONG,
                        TagTimerPayload::remainingMs,
                        TagTimerPayload::new
                );

        @Override
        public CustomPayload.Id<? extends CustomPayload> getId() {
            return TYPE;
        }
    }

    private CombatLogNetworking() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(TagTimerPayload.TYPE, TagTimerPayload.CODEC);
    }

    public static void sendTagTime(ServerPlayerEntity player, long remainingMs) {
        if (player == null) return;
        ServerPlayNetworking.send(player, new TagTimerPayload(remainingMs));
    }
}