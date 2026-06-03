package com.combatlog.commands;

import com.combatlog.CombatStateManager;
import com.combatlog.ConfigLoader;
import com.combatlog.LogWriter;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class CombatLogCommand {

    public static void register(CombatStateManager stateManager, ConfigLoader config, LogWriter logWriter) {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("combatlog")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> executeCombatLog(ctx.getSource()))
                .then(Commands.literal("clear")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> clearCombatLog(ctx.getSource()))
                        .then(Commands.argument("player", EntityArgument.player())
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> clearPlayerCombatLog(
                                        ctx.getSource(),
                                        EntityArgument.getPlayer(ctx, "player")))
                        )
                )
                .then(Commands.literal("list")
                        .requires(source -> source.hasPermission(2))
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