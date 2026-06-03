package com.combatlog;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import com.combatlog.commands.CombatLogCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombatLogMod implements ModInitializer {
    public static final String MOD_ID = "combatlog";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static CombatStateManager stateManager;
    private static LogWriter logWriter;
    private static ConfigLoader config;

    @Override
    public void onInitialize() {
        LOGGER.info("[CombatLog] Initialising...");
        config = new ConfigLoader();
        config.load();
        logWriter = new LogWriter();
        stateManager = new CombatStateManager(config, logWriter);
        CombatLogNetworking.register(); // ← ADD THIS
        registerEvents();
        CombatLogCommand.register(stateManager, config, logWriter);
        LOGGER.info("[CombatLog] Ready.");
    }

    private void registerEvents() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(this::onEntityDamage);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                onPlayerDisconnect(handler.player, server));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                stateManager.tickExpiry(server);
            }
        });
    }

    private void onEntityDamage(LivingEntity entity, DamageSource source,
                                float baseDamage, float damage, boolean blocked) {
        if (!(entity instanceof ServerPlayer victim)) return;
        if (!(source.getEntity() instanceof ServerPlayer attacker)) return;
        if (victim.equals(attacker)) return;
        stateManager.tag(attacker, victim);
    }

    private void onPlayerDisconnect(ServerPlayer player, MinecraftServer server) {
        stateManager.handleDisconnect(player, server);
    }

    public static CombatStateManager getStateManager() { return stateManager; }
    public static ConfigLoader getConfig() { return config; }
    public static LogWriter getLogWriter() { return logWriter; }
}