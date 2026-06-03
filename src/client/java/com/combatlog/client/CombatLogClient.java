package com.combatlog.client;

import com.combatlog.CombatLogNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

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
        HudRenderCallback.EVENT.register(CombatLogClient::renderHud);
    }

    private static void renderHud(GuiGraphics graphics, float tickDelta) {
        long remainingMs = tagEndsAtMs - System.currentTimeMillis();
        if (remainingMs <= 0) return;

        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) return;

        int seconds = (int) Math.ceil(remainingMs / 1000.0);
        String text = String.valueOf(seconds);
        Font font = client.font;
        int width = client.getWindow().getGuiScaledWidth();
        int height = client.getWindow().getGuiScaledHeight();
        int x = (width - font.width(text)) / 2;
        int y = height - 49;
        float scale = 0.9f;
        var pose = graphics.pose();
        pose.pushPose();
        pose.scale(scale, scale, 1.0f);
        int scaledX = Math.round(x / scale);
        int scaledY = Math.round(y / scale);
        graphics.drawString(font, text, scaledX, scaledY, 0xFFFF5555, false);
        pose.popPose();
    }
}