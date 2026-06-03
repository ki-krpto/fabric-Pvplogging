package net.fabricmc.example.combatlog.commands;
 
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.example.combatlog.CombatLogMod;
import net.fabricmc.example.combatlog.CombatState;
import net.fabricmc.example.combatlog.CombatStateManager;
import net.fabricmc.example.combatlog.ConfigLoader;
import net.fabricmc.example.combatlog.LogWriter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
 
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
 
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
                    .then(literal("status")
                        .executes(ctx -> statusSelf(ctx))
                        .then(argument("player", EntityArgument.player())
                            .requires(src -> src.getPermissionLevel() >= 2)
                            .executes(ctx -> statusOther(ctx))))
 
                    .then(literal("reload")
                        .requires(src -> src.getPermissionLevel() >= 2)
                        .executes(ctx -> reload(ctx)))
 
                    .then(literal("exempt")
                        .requires(src -> src.getPermissionLevel() >= 2)
                        .then(argument("player", EntityArgument.player())
                            .executes(ctx -> toggleExempt(ctx))))
 
                    .then(literal("cleartag")
                        .requires(src -> src.getPermissionLevel() >= 2)
                        .then(argument("player", EntityArgument.player())
                            .executes(ctx -> clearTag(ctx))))
 
                    .then(literal("version")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                () -> Component.literal("§6[CombatLog] §7v1.0.0 — Fabric 26.1"), false);
                            return 1;
                        }))
            )
        );
    }
 
    private static int statusSelf(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.literal("Run this as a player."));
            return 0;
        }
        sendStatus(ctx.getSource(), player);
        return 1;
    }
 
    private static int statusOther(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            sendStatus(ctx.getSource(), target);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }
    }
 
    private static void sendStatus(CommandSourceStack src, ServerPlayer player) {
        CombatState state = stateManager.getState(player.getUUID());
        boolean exempt    = stateManager.isExempt(player.getUUID());
        if (state.isTagged()) {
            long secs = state.msRemaining() / 1000;
            src.sendSuccess(() -> Component.literal(
                "§6[CombatLog] §c" + player.getName().getString()
                + " §7is in combat. §c" + secs + "s §7remaining."
                + (exempt ? " §a(exempt)" : "")), false);
        } else {
            src.sendSuccess(() -> Component.literal(
                "§6[CombatLog] §a" + player.getName().getString()
                + " §7is not in combat."
                + (exempt ? " §a(exempt)" : "")), false);
        }
    }
 
    private static int reload(CommandContext<CommandSourceStack> ctx) {
        config.load();
        ctx.getSource().sendSuccess(
            () -> Component.literal("§6[CombatLog] §7Config reloaded."), true);
        return 1;
    }
 
    private static int toggleExempt(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            boolean nowExempt = !stateManager.isExempt(target.getUUID());
            stateManager.setExempt(target.getUUID(), nowExempt);
            String state = nowExempt ? "§aexempted" : "§cremoved from exemptions";
            ctx.getSource().sendSuccess(
                () -> Component.literal("§6[CombatLog] §7" + target.getName().getString()
                    + " " + state + "§7."), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }
    }
 
    private static int clearTag(CommandContext<CommandSourceStack> ctx) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            stateManager.forceClearTag(target.getUUID());
            target.sendSystemMessage(Component.literal("§a[CombatLog] §7Your combat tag was cleared by an admin."));
            ctx.getSource().sendSuccess(
                () -> Component.literal("§6[CombatLog] §7Cleared tag for §c"
                    + target.getName().getString() + "§7."), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Player not found."));
            return 0;
        }
    }
}
 
