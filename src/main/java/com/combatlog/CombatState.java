package net.fabricmc.example.combatlog;

import java.util.UUID;

/**
 * Immutable-ish data bag for one player's combat state.
 * All time values are System.currentTimeMillis().
 */
public class CombatState {

    private final UUID playerUuid;
    private volatile long taggedUntil;   // epoch ms when tag expires (0 = not tagged)
    private volatile UUID lastAttacker;  // may be null
    private volatile UUID lastVictim;    // may be null

    public CombatState(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.taggedUntil = 0;
    }

    // -------------------------------------------------------------------------
    // Tagging
    // -------------------------------------------------------------------------

    public void tag(long durationMs, UUID attacker, UUID victim) {
        this.taggedUntil  = System.currentTimeMillis() + durationMs;
        this.lastAttacker = attacker;
        this.lastVictim   = victim;
    }

    public void clearTag() {
        this.taggedUntil  = 0;
        this.lastAttacker = null;
        this.lastVictim   = null;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    public boolean isTagged() {
        return taggedUntil > System.currentTimeMillis();
    }

    public long msRemaining() {
        return Math.max(0, taggedUntil - System.currentTimeMillis());
    }

    public UUID getPlayerUuid()   { return playerUuid; }
    public UUID getLastAttacker() { return lastAttacker; }
    public UUID getLastVictim()   { return lastVictim; }
    public long getTaggedUntil()  { return taggedUntil; }
}
