package net.fabricmc.example.combatlog;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatStateManager {

    private final Map<UUID, CombatState> states = new ConcurrentHashMap<>();
    // Players explicitly exempted by an admin for this session
    private final Set<UUID> exemptions = ConcurrentHashMap.newKeySet();

    private final ConfigLoader config;
    private final LogWriter logWriter;

    public CombatStateManager(ConfigLoader config, LogWriter logWriter) {
        this.config    = config;
        this.logWriter = logWriter;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Tag both the attacker and the victim. */
    public void tag(ServerPlayerEntity attacker, ServerPlayerEntity victim) {
        if (exemptions.contains(attacker.getUuid()) ||
            exemptions.contains(victim.getUuid())) return;

        long durationMs = config.getTagDurationSeconds() * 1000L;

        getOrCreate(attacker.getUuid()).tag(durationMs, attacker.getUuid(), victim.getUuid());
        getOrCreate(victim.getUuid()).tag(durationMs, attacker.getUuid(), victim.getUuid());

        if (config.isNotifyAttacker()) {
            attacker.sendMessage(Text.literal(
                    "§c[Combat] §7You are in combat for §c"
                    + config.getTagDurationSeconds() + "s§7."), true); // action bar
        }
        if (config.isNotifyAttacker()) {
            victim.sendMessage(Text.literal(
                    "§c[Combat] §7You are in combat for §c"
                    + config.getTagDurationSeconds() + "s§7."), true);
        }
    }

    /** Called every second from the server tick event. */
    public void tickExpiry(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, CombatState> entry : states.entrySet()) {
            CombatState state = entry.getValue();
            if (state.isTagged()) continue; // still active
            if (state.getTaggedUntil() == 0) continue; // never tagged

            // Tag just expired — notify the player if online
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            if (player != null) {
                player.sendMessage(Text.literal("§a[Combat] §7You are no longer in combat."), true);
            }
            state.clearTag();
        }
    }

    /** Called on ServerPlayConnectionEvents.DISCONNECT. */
    public void handleDisconnect(ServerPlayerEntity player, MinecraftServer server) {
        CombatState state = states.get(player.getUuid());
        if (state == null || !state.isTagged()) return;

        long msRemaining = state.msRemaining();
        String attackerName = resolveUsername(server, state.getLastAttacker());

        // Log the event
        logWriter.logCombatLog(player, state, attackerName);

        // Punish
        applyPunishment(player, server, state);

        state.clearTag();
    }

    // -------------------------------------------------------------------------
    // Admin helpers (used by commands)
    // -------------------------------------------------------------------------

    public CombatState getState(UUID uuid) {
        return states.getOrDefault(uuid, new CombatState(uuid));
    }

    public void forceClearTag(UUID uuid) {
        CombatState state = states.get(uuid);
        if (state != null) state.clearTag();
    }

    public void setExempt(UUID uuid, boolean exempt) {
        if (exempt) exemptions.add(uuid);
        else        exemptions.remove(uuid);
    }

    public boolean isExempt(UUID uuid) {
        return exemptions.contains(uuid);
    }

    public void cleanup(UUID uuid) {
        states.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Internals
    // -------------------------------------------------------------------------

    private CombatState getOrCreate(UUID uuid) {
        return states.computeIfAbsent(uuid, CombatState::new);
    }

    private String resolveUsername(MinecraftServer server, UUID uuid) {
        if (uuid == null) return "unknown";
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
        return p != null ? p.getName().getString() : uuid.toString();
    }

    private void applyPunishment(ServerPlayerEntity player,
                                 MinecraftServer server,
                                 CombatState state) {
        switch (config.getPunishment()) {
            case KILL -> server.execute(player::kill);

            case LIGHTNING -> server.execute(() -> {
                World world = player.getWorld();
                BlockPos pos = player.getBlockPos();
                LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
                bolt.refreshPositionAfterTeleport(pos.getX(), pos.getY(), pos.getZ());
                world.spawnEntity(bolt);
                player.kill(); // lightning alone doesn't always kill
            });

            case COMMAND -> {
                String cmd = config.getPunishmentCommand()
                        .replace("%player%", player.getName().getString());
                server.execute(() -> server.getCommandManager()
                        .executeWithPrefix(server.getCommandSource(), cmd));
            }

            case NONE -> {
                // Log only — no further action
            }
        }

        // Notify all online players if configured
        if (config.isNotifyBystanders()) {
            server.getPlayerManager().broadcast(
                Text.literal("§c[Combat] §7" + player.getName().getString()
                    + " logged out while in combat!"), false);
        }
    }
}
