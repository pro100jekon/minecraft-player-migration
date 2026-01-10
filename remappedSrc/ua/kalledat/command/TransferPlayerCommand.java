package ua.kalledat.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.ScoreHolder;
import java.io.IOException;
import java.util.Map;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static ua.kalledat.PlayerMigration.*;

public class TransferPlayerCommand {

    public static void registerCommand() {
        CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) ->
                        dispatcher.register(createTransferPlayerCommand()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> createTransferPlayerCommand() {
        return literal("transferplayer")
                .requires(Permissions.require(MOD_ID + ".command.transferplayer", 4))
                .then(argument("old_nickname", StringArgumentType.word())
                        .then(argument("new_nickname", StringArgumentType.word())
                                .executes(createPlayerMigration())));
    }

    private static Command<CommandSourceStack> createPlayerMigration() {
        return ctx -> {
            var oldNickname = StringArgumentType.getString(ctx, "old_nickname");
            var newNickname = StringArgumentType.getString(ctx, "new_nickname");
            try {
                playerMigrationRepo.saveNewPlayerMigration(Map.entry(oldNickname, newNickname));
            } catch (IOException e) {
                ctx.getSource().sendSuccess(() ->
                        Component.translatable("player-migration.rename-error", oldNickname), true);
                LOGGER.error("Error while saving player migration", e);
                return -1;
            }
            transferScoreboardScores(ctx.getSource().getServer(), oldNickname, newNickname);
            LOGGER.info("Player '{}' has been renamed to '{}'", oldNickname, newNickname);
            ctx.getSource().sendSuccess(() ->
                    Component.translatable("player-migration.rename-success",oldNickname, newNickname), true);
            var player = ctx.getSource().getServer().getPlayerList().getPlayerByName(oldNickname);
            if (player != null) {
                player.sendSystemMessage(Component.translatable("player-migration.renamed-player-notification", newNickname));
            }
            return 0;
        };
    }

    private static void transferScoreboardScores(MinecraftServer server, String oldName, String newName) {
        ServerScoreboard scoreboard = server.getScoreboard();
        ScoreHolder oldHolder = ScoreHolder.forNameOnly(oldName);
        ScoreHolder newHolder = ScoreHolder.forNameOnly(newName);

        var oldScores = scoreboard.listPlayerScores(oldHolder);
        if (oldScores.isEmpty()) {
            LOGGER.info("No scoreboard scores found for '{}'", oldName);
            return;
        }

        int transferred = 0;
        for (var entry : oldScores.object2IntEntrySet()) {
            var objective = entry.getKey();
            int score = entry.getIntValue();
            scoreboard.getOrCreatePlayerScore(newHolder, objective, true).set(score);
            transferred++;
        }
        LOGGER.info("Transferred {} scoreboard scores from '{}' to '{}'", transferred, oldName, newName);
    }
}
