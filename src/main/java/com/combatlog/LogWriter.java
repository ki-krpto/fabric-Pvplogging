package net.fabricmc.example.combatlog;
 
import net.minecraft.server.level.ServerPlayer;
 
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
 
public class LogWriter {
 
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Path LOG_DIR = Paths.get("config", "combatlog", "logs");
 
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "combatlog-writer");
        t.setDaemon(true);
        return t;
    });
 
    public LogWriter() {
        try {
            Files.createDirectories(LOG_DIR);
        } catch (IOException e) {
            CombatLogMod.LOGGER.error("[CombatLog] Failed to create log directory", e);
        }
    }
 
    public void logCombatLog(ServerPlayer player, CombatState state, String attackerName) {
        String json = buildJson(
            "combat_log",
            player.getName().getString(),
            player.getStringUUID(),
            attackerName,
            state.getLastAttacker() != null ? state.getLastAttacker().toString() : "",
            state.msRemaining()
        );
        write(json);
    }
 
    public void logPvpEvent(String event, String playerName, String playerUuid,
                            String otherName, String otherUuid) {
        String json = String.format(
            "{\"t\":%d,\"event\":\"%s\",\"player\":\"%s\",\"uuid\":\"%s\"," +
            "\"other\":\"%s\",\"other_uuid\":\"%s\"}",
            System.currentTimeMillis(), escape(event),
            escape(playerName), escape(playerUuid),
            escape(otherName), escape(otherUuid)
        );
        write(json);
    }
 
    public void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
 
    private String buildJson(String event, String player, String uuid,
                             String attacker, String attackerUuid, long msRemaining) {
        return String.format(
            "{\"t\":%d,\"event\":\"%s\",\"player\":\"%s\",\"uuid\":\"%s\"," +
            "\"attacker\":\"%s\",\"attacker_uuid\":\"%s\",\"tagged_ms_remaining\":%d}",
            System.currentTimeMillis(), escape(event),
            escape(player), escape(uuid),
            escape(attacker), escape(attackerUuid),
            msRemaining
        );
    }
 
    private void write(String json) {
        executor.submit(() -> {
            Path file = LOG_DIR.resolve(LocalDate.now().format(DATE_FMT) + ".log");
            try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND)) {
                bw.write(json);
                bw.newLine();
            } catch (IOException e) {
                CombatLogMod.LOGGER.error("[CombatLog] Failed to write log entry", e);
            }
        });
    }
 
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
