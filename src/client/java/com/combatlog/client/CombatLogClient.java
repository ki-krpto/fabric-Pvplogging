package com.combatlog.client;

import com.combatlog.CombatLogNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class CombatLogClient implements ClientModInitializer {

    private static volatile long tagEndsAtMs = 0;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                CombatLogNetworking.TagTimerPayload.TYPE,
                (payload, context) -> {
                    long remainingMs = payload.remainingMs();
                    context.client().execute(() -> {
                        tagEndsAtMs = remainingMs <= 0
                                ? 0
                                : System.currentTimeMillis() + remainingMs;
                    });
                }
        );

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("combatlog", "tag_timer"),
                CombatLogClient::renderHud
        );
    }

    private static void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        long remainingMs = tagEndsAtMs - System.currentTimeMillis();
        if (remainingMs <= 0) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;

        int seconds = (int) Math.ceil(remainingMs / 1000.0);
        String text = "Combat: " + seconds + "s";

        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int x = (width - client.font.width(text)) / 2;
        int y = height - 49;

        graphics.fill(x - 2, y - 2, x + client.font.width(text) + 2, y + 10, 0x88000000);
        graphics.drawString(client.font, text, x, y, 0xFFFF5555, true);
    }
}