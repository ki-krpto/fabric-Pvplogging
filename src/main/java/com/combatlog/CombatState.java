package com.combatlog;

import java.util.UUID;

public class CombatState {

    private final UUID playerUuid;
    private volatile long taggedUntil;
    private volatile UUID lastAttacker;
    private volatile UUID lastVictim;

    public CombatState(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.taggedUntil = 0;
    }

    public void tag(long durationMs, UUID attacker, UUID victim) {
        this.taggedUntil = System.currentTimeMillis() + durationMs;
        this.lastAttacker = attacker;
        this.lastVictim = victim;
    }

    public void clearTag() {
        this.taggedUntil = 0;
        this.lastAttacker = null;
        this.lastVictim = null;
    }

    public boolean isTagged() {
        return taggedUntil > System.currentTimeMillis();
    }

    public long msRemaining() {
        return Math.max(0, taggedUntil - System.currentTimeMillis());
    }

    public UUID getPlayerUuid() { return playerUuid; }
    public UUID getLastAttacker() { return lastAttacker; }
    public UUID getLastVictim() { return lastVictim; }
    public long getTaggedUntil() { return taggedUntil; }
}