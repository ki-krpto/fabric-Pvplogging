package com.combatlog;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;

public class ConfigLoader {

    public enum Punishment { KILL, LIGHTNING, COMMAND, NONE }

    private static final Path CONFIG_FILE =
            Paths.get("config", "combatlog", "config.properties");

    private int tagDurationSeconds = 15;
    private int gracePeriodSeconds = 2;
    private Punishment punishment = Punishment.KILL;
    private String punishmentCommand = "say %player% logged out in combat!";
    private boolean notifyAttacker = true;
    private boolean notifyBystanders = false;
    private boolean logPvpEvents = true;

    public void load() {
        ensureDefaults();
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            CombatLogMod.LOGGER.warn("[CombatLog] Could not read config, using defaults. ({})", e.getMessage());
            return;
        }

        tagDurationSeconds = parseInt(props, "tag_duration_seconds", tagDurationSeconds);
        gracePeriodSeconds = parseInt(props, "grace_period_seconds", gracePeriodSeconds);
        notifyAttacker = parseBool(props, "notify_attacker", notifyAttacker);
        notifyBystanders = parseBool(props, "notify_bystanders", notifyBystanders);
        logPvpEvents = parseBool(props, "log_pvp_events", logPvpEvents);
        punishmentCommand = props.getProperty("punishment_command", punishmentCommand);

        String punishStr = props.getProperty("punishment", punishment.name()).toUpperCase();
        try {
            punishment = Punishment.valueOf(punishStr);
        } catch (IllegalArgumentException ex) {
            CombatLogMod.LOGGER.warn("[CombatLog] Unknown punishment '{}', defaulting to KILL", punishStr);
            punishment = Punishment.KILL;
        }

        CombatLogMod.LOGGER.info("[CombatLog] Config loaded. Tag={}s, Punishment={}",
                tagDurationSeconds, punishment);
    }

    private void ensureDefaults() {
        if (Files.exists(CONFIG_FILE)) return;
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            String defaults =
                    "# CombatLog configuration\n" +
                    "tag_duration_seconds=15\n\n" +
                    "grace_period_seconds=2\n\n" +
                    "punishment=KILL\n\n" +
                    "punishment_command=say %player% logged out in combat!\n\n" +
                    "notify_attacker=true\n\n" +
                    "notify_bystanders=false\n\n" +
                    "log_pvp_events=true\n";
            Files.writeString(CONFIG_FILE, defaults, StandardCharsets.UTF_8);
        } catch (IOException e) {
            CombatLogMod.LOGGER.error("[CombatLog] Failed to write default config", e);
        }
    }

    private int parseInt(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, String.valueOf(def)).trim()); }
        catch (NumberFormatException e) { return def; }
    }

    private boolean parseBool(Properties p, String key, boolean def) {
        String v = p.getProperty(key);
        if (v == null) return def;
        return v.trim().equalsIgnoreCase("true");
    }

    public int getTagDurationSeconds() { return tagDurationSeconds; }
    public int getGracePeriodSeconds() { return gracePeriodSeconds; }
    public Punishment getPunishment() { return punishment; }
    public String getPunishmentCommand() { return punishmentCommand; }
    public boolean isNotifyAttacker() { return notifyAttacker; }
    public boolean isNotifyBystanders() { return notifyBystanders; }
    public boolean isLogPvpEvents() { return logPvpEvents; }
}