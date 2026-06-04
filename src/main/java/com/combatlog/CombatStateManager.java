package com.combatlog;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CombatStateManager {

    private final Map<UUID, CombatState> states = new ConcurrentHashMap<>();
    private final Set<UUID> exemptions = ConcurrentHashMap.newKeySet();

    private final ConfigLoader config;
    private final LogWriter logWriter;

    public CombatStateManager(ConfigLoader config, LogWriter logWriter) {
        this.config = config;
        this.logWriter = logWriter;
    }

    public void tag(ServerPlayer attacker, ServerPlayer victim) {
        if (exemptions.contains(attacker.getUUID()) ||
                exemptions.contains(victim.getUUID())) return;

        long durationMs = config.getTagDurationSeconds() * 1000L;

        CombatState attackerState = getOrCreate(attacker.getUUID());
        CombatState victimState = getOrCreate(victim.getUUID());

        boolean attackerWasTagged = attackerState.isTagged();
        boolean victimWasTagged = victimState.isTagged();

        attackerState.tag(durationMs, attacker.getUUID(), victim.getUUID());
        victimState.tag(durationMs, attacker.getUUID(), victim.getUUID());

        if (config.isNotifyAttacker() && !attackerWasTagged) {
            attacker.sendSystemMessage(Component.literal(
                    "§c[Combat] §7You are in combat for §c"
                            + config.getTagDurationSeconds() + "s§7."));
        }
        if (config.isNotifyAttacker() && !victimWasTagged) {
            victim.sendSystemMessage(Component.literal(
                    "§c[Combat] §7You are in combat for §c"
                            + config.getTagDurationSeconds() + "s§7."));
        }

        CombatLogNetworking.sendTagTime(attacker, attackerState.msRemaining());
        CombatLogNetworking.sendTagTime(victim, victimState.msRemaining());
    }

    public void tickExpiry(MinecraftServer server) {
        for (Map.Entry<UUID, CombatState> entry : states.entrySet()) {
            CombatState state = entry.getValue();
            if (!state.isTagged()) continue; // skip players not in combat

            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());

            if (state.msRemaining() <= 0) {
                // Tag has expired — notify and clear
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                            "§a[Combat] §7You are no longer in combat."));
                    CombatLogNetworking.sendTagTime(player, 0);
                }
                state.clearTag();
            } else {
                // Still tagged — keep client timer in sync
                if (player != null) {
                    CombatLogNetworking.sendTagTime(player, state.msRemaining());
                }
            }
        }
    }

    public void handleDisconnect(ServerPlayer player, MinecraftServer server) {
        CombatState state = states.get(player.getUUID());
        if (state == null || !state.isTagged()) return;

        String attackerName = resolveUsername(server, state.getLastAttacker());
        logWriter.logCombatLog(player, state, attackerName);
        applyPunishment(player, server, state);
        state.clearTag();
    }

    public CombatState getState(UUID uuid) {
        return states.getOrDefault(uuid, new CombatState(uuid));
    }

    public void forceClearTag(UUID uuid) {
        CombatState state = states.get(uuid);
        if (state != null) state.clearTag();
    }

    public void setExempt(UUID uuid, boolean exempt) {
        if (exempt) exemptions.add(uuid);
        else exemptions.remove(uuid);
    }

    public boolean isExempt(UUID uuid) {
        return exemptions.contains(uuid);
    }

    public void cleanup(UUID uuid) {
        states.remove(uuid);
    }

    private CombatState getOrCreate(UUID uuid) {
        return states.computeIfAbsent(uuid, CombatState::new);
    }

    private String resolveUsername(MinecraftServer server, UUID uuid) {
        if (uuid == null) return "unknown";
        ServerPlayer p = server.getPlayerList().getPlayer(uuid);
        return p != null ? p.getName().getString() : uuid.toString();
    }

    private void applyPunishment(ServerPlayer player, MinecraftServer server, CombatState state) {
        switch (config.getPunishment()) {
            case KILL -> server.execute(() -> player.kill((ServerLevel) player.level()));

            case LIGHTNING -> server.execute(() -> {
                ServerLevel world = (ServerLevel) player.level();
                BlockPos pos = player.blockPosition();
                LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, world);
                bolt.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                world.addFreshEntity(bolt);
                player.kill(world);
            });

            case COMMAND -> {
                String cmd = config.getPunishmentCommand()
                        .replace("%player%", player.getName().getString());
                server.execute(() -> server.getCommands()
                        .performPrefixedCommand(server.createCommandSourceStack(), cmd));
            }

            case NONE -> { }
        }

        if (config.isNotifyBystanders()) {
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§c[Combat] §7" + player.getName().getString()
                            + " logged out while in combat!"), false);
        }
    }
}