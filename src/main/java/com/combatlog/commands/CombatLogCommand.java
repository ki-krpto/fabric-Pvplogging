package net.fabricmc.example.combatlog.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.example.combatlog.CombatLogMod;
import net.fabricmc.example.combatlog.CombatState;
import net.fabricmc.example.combatlog.CombatStateManager;
import net.fabricmc.example.combatlog.ConfigLoader;
import net.fabricmc.example.combatlog.LogWriter;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Collection;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/**
 * Registers the /combatlog command tree.
 *
 * Subcommands:
 *   /combatlog status [player]    — show tag state (op level 0 for self, 2 for others)
 *   /combatlog reload             — hot-reload config (op level 2)
 *   /combatlog exempt <player>    — toggle session exemption (op level 2)
 *   /combatlog cleartag <player>  — force-clear a tag (op level 2)
 *   /combatlog version            — print mod version (op level 0)
 */
public class CombatLogCommand {

    private static CombatStateManager stateManager;
    private static ConfigLoader config;
    private static LogWriter logWriter;

    public static void register(CombatStateManager sm, ConfigLoader cfg, LogWriter lw) {
        stateManager = sm;
        config       = cfg;
        logWriter    = lw;

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(
                literal("combatlog")
                    // /combatlog status
                    .then(literal("status")
                        .executes(ctx -> statusSelf(ctx))
                        .then(argument("player", EntityArgumentType.player())
                            .requires(src -> src.hasPermissionLevel(2))
                            .executes(ctx -> statusOther(ctx))))

                    // /combatlog reload
                    .then(literal("reload")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> reload(ctx)))

                    // /combatlog exempt <player>
                    .then(literal("exempt")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(argument("player", EntityArgumentType.player())
                            .executes(ctx -> toggleExempt(ctx))))

                    // /combatlog cleartag <player>
                    .then(literal("cleartag")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(argument("player", EntityArgumentType.player())
                            .executes(ctx -> clearTag(ctx))))

                    // /combatlog version
                    .then(literal("version")
                        .executes(ctx -> {
                            ctx.getSource().sendFeedback(
                                () -> Text.literal("§6[CombatLog] §7v1.0.0 — Fabric 1.21"), false);
                            return 1;
                        }))
            )
        );
    }

    // -------------------------------------------------------------------------
    // Subcommand handlers
    // -------------------------------------------------------------------------

    private static int statusSelf(CommandContext<ServerCommandSource> ctx) {
        ServerPlayerEntity player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendError(Text.literal("Run this as a player."));
            return 0;
        }
        sendStatus(ctx.getSource(), player);
        return 1;
    }

    private static int statusOther(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
            sendStatus(ctx.getSource(), target);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Player not found."));
            return 0;
        }
    }

    private static void sendStatus(ServerCommandSource src, ServerPlayerEntity player) {
        CombatState state = stateManager.getState(player.getUuid());
        boolean exempt    = stateManager.isExempt(player.getUuid());
        if (state.isTagged()) {
            long secs = state.msRemaining() / 1000;
            src.sendFeedback(() -> Text.literal(
                "§6[CombatLog] §c" + player.getName().getString()
                + " §7is in combat. §c" + secs + "s §7remaining."
                + (exempt ? " §a(exempt)" : "")), false);
        } else {
            src.sendFeedback(() -> Text.literal(
                "§6[CombatLog] §a" + player.getName().getString()
                + " §7is not in combat."
                + (exempt ? " §a(exempt)" : "")), false);
        }
    }

    private static int reload(CommandContext<ServerCommandSource> ctx) {
        config.load();
        ctx.getSource().sendFeedback(
            () -> Text.literal("§6[CombatLog] §7Config reloaded."), true);
        return 1;
    }

    private static int toggleExempt(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
            boolean nowExempt = !stateManager.isExempt(target.getUuid());
            stateManager.setExempt(target.getUuid(), nowExempt);
            String state = nowExempt ? "§aexempted" : "§cremoved from exemptions";
            ctx.getSource().sendFeedback(
                () -> Text.literal("§6[CombatLog] §7" + target.getName().getString()
                    + " " + state + "§7."), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Player not found."));
            return 0;
        }
    }

    private static int clearTag(CommandContext<ServerCommandSource> ctx) {
        try {
            ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
            stateManager.forceClearTag(target.getUuid());
            target.sendMessage(Text.literal("§a[CombatLog] §7Your combat tag was cleared by an admin."), false);
            ctx.getSource().sendFeedback(
                () -> Text.literal("§6[CombatLog] §7Cleared tag for §c"
                    + target.getName().getString() + "§7."), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendError(Text.literal("Player not found."));
            return 0;
        }
    }
}
