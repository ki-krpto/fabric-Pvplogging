package com.combatlog;

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class CombatLogNetworking {
    public static final ResourceLocation TAG_TIMER_PACKET =
            ResourceLocation.fromNamespaceAndPath(CombatLogMod.MOD_ID, "tag_timer");

    private CombatLogNetworking() { }

    public static void sendTagTime(ServerPlayer player, long remainingMs) {
        if (player == null) return;
        FriendlyByteBuf buf = FriendlyByteBufs.create();
        buf.writeLong(remainingMs);
        ServerPlayNetworking.send(player, TAG_TIMER_PACKET, buf);
    }
}