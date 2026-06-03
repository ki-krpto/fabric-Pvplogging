package com.combatlog.commands;

import com.combatlog.CombatStateManager;
import com.combatlog.ConfigLoader;
import com.combatlog.LogWriter;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CombatLogCommand {

    public static void register(CombatStateManager stateManager, ConfigLoader config, LogWriter logWriter) {
        // Registration will be handled via Fabric's CommandRegistrationCallback
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("combatlog")
                .requires(src -> src.hasPermission(2))
                .executes(ctx -> executeCombatLog(ctx.getSource()))
                .then(Commands.literal("clear")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> clearCombatLog(ctx.getSource()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(src -> src.hasPermission(2))
                                .executes(ctx -> clearPlayerCombatLog(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))
                        )
                )
                .then(Commands.literal("list")
                        .requires(src -> src.hasPermission(2))
                        .executes(ctx -> listCombatLog(ctx.getSource()))
                )
        );
    }

    private static int executeCombatLog(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("CombatLog command executed"), false);
        return 1;
    }

    private static int clearCombatLog(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("CombatLog cleared"), false);
        return 1;
    }

    private static int clearPlayerCombatLog(CommandSourceStack source, ServerPlayer player) {
        source.sendSuccess(() -> Component.literal("Cleared CombatLog for " + player.getName().getString()), false);
        return 1;
    }

    private static int listCombatLog(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("CombatLog list"), false);
        return 1;
    }
}