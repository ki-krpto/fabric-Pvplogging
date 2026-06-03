package com.ki_krpto.combatlog.commands;

import com.ki_krpto.combatlog.CombatLog;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.entity.player.PlayerEntity;

public class CombatLogCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("combatlog")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> executeCombatLog(ctx.getSource()))
                .then(CommandManager.literal("clear")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> clearCombatLog(ctx.getSource()))
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> clearPlayerCombatLog(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))
                        )
                )
                .then(CommandManager.literal("list")
                        .requires(src -> src.hasPermission(2))
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

    private static int clearPlayerCombatLog(ServerCommandSource source, PlayerEntity player) {
        source.sendFeedback(() -> Text.literal("Cleared CombatLog for " + player.getName().getString()), false);
        return 1;
    }

    private static int listCombatLog(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("CombatLog list"), false);
        return 1;
    }
}
