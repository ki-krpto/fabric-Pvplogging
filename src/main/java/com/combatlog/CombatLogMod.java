package net.fabricmc.example.combatlog;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.example.combatlog.commands.CombatLogCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombatLogMod implements ModInitializer {

    public static final String MOD_ID = "combatlog";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Singleton managers — initialised once the server is available
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

        registerEvents();
        CombatLogCommand.register(stateManager, config, logWriter);

        LOGGER.info("[CombatLog] Ready.");
    }

    // -------------------------------------------------------------------------
    // Event wiring
    // -------------------------------------------------------------------------

    private void registerEvents() {

        // Tag both players whenever one damages the other
        ServerLivingEntityEvents.AFTER_DAMAGE.register(this::onEntityDamage);

        // Check for combat-log on disconnect
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                onPlayerDisconnect(handler.player, server));

        // Tick-based tag expiry check (runs every second = 20 ticks)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) {
                stateManager.tickExpiry(server);
            }
        });
    }

    private void onEntityDamage(LivingEntity entity, DamageSource source,
                                float baseDamage, float damage, boolean blocked) {
        if (!(entity instanceof ServerPlayerEntity victim)) return;
        if (!(source.getAttacker() instanceof ServerPlayerEntity attacker)) return;
        if (victim.equals(attacker)) return; // ignore self-damage

        stateManager.tag(attacker, victim);
    }

    private void onPlayerDisconnect(ServerPlayerEntity player, MinecraftServer server) {
        stateManager.handleDisconnect(player, server);
    }

    // -------------------------------------------------------------------------
    // Static accessors (used by command class)
    // -------------------------------------------------------------------------

    public static CombatStateManager getStateManager() { return stateManager; }
    public static ConfigLoader getConfig()              { return config; }
    public static LogWriter getLogWriter()              { return logWriter; }
}
