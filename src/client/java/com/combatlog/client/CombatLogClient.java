package com.combatlog.client;

import com.combatlog.CombatLogNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

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

    private static void renderHud(DrawContext drawContext, float tickDelta) {
        long remainingMs = tagEndsAtMs - System.currentTimeMillis();
        if (remainingMs <= 0) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;

        int seconds = (int) Math.ceil(remainingMs / 1000.0);
        String text = String.valueOf(seconds);

        TextRenderer textRenderer = client.textRenderer;
        int width  = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        int x = (width - textRenderer.getWidth(text)) / 2;
        int y = height - 49;

        float scale = 0.9f;
        var matrices = drawContext.getMatrices();
        matrices.push();
        matrices.scale(scale, scale, 1.0f);
        drawContext.drawText(
                textRenderer,
                text,
                Math.round(x / scale),
                Math.round(y / scale),
                0xFFFF5555,
                false
        );
        matrices.pop();
    }
}