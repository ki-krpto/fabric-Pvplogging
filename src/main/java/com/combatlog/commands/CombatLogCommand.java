package com.combatlog.commands;

import com.combatlog.CombatStateManager;
import com.combatlog.ConfigLoader;
import com.combatlog.LogWriter;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class CombatLogCommand {

    public static void register(CombatStateManager stateManager, ConfigLoader config, LogWriter logWriter) {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                register(dispatcher));
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("combatlog")
                .requires(src -> src.hasPermissionLevel(2))
                .executes(ctx -> executeCombatLog(ctx.getSource()))
                .then(CommandManager.literal("clear")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> clearCombatLog(ctx.getSource()))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .requires(src -> src.hasPermissionLevel(2))
                                .executes(ctx -> clearPlayerCombatLog(
                                        ctx.getSource(),
                                        EntityArgumentType.getPlayer(ctx, "player")))
                        )
                )
                .then(CommandManager.literal("list")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes(ctx -> listCombatLog(ctx.getSource()))
                )
        );
    }

    private static int executeCombatLog(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("CombatLog command executed"), false);
        return 1;
    }

    private static int clearCombatLog(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("CombatLog cleared"), false);
        return 1;
    }

    private static int clearPlayerCombatLog(ServerCommandSource source, ServerPlayerEntity player) {
        source.sendFeedback(() -> Text.literal("Cleared CombatLog for " + player.getName().getString()), false);
        return 1;
    }

    private static int listCombatLog(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("CombatLog list"), false);
        return 1;
    }
}
