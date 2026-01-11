package ua.kalledat.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static ua.kalledat.PlayerMigration.LOGGER;
import static ua.kalledat.PlayerMigration.MOD_ID;
import static ua.kalledat.PlayerMigration.playerMigrationRepo;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.util.Map;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.scores.ScoreHolder;

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
                .executes(TransferPlayerCommand::createPlayerMigration)));
  }

  private static int createPlayerMigration(CommandContext<CommandSourceStack> ctx) {
    var oldNickname = StringArgumentType.getString(ctx, "old_nickname");
    var newNickname = StringArgumentType.getString(ctx, "new_nickname");
    var successful = false;
    try {
      successful = playerMigrationRepo.saveNewPlayerMigration(Map.entry(oldNickname, newNickname));
    } catch (IOException e) {
      ctx.getSource().sendFailure(
          Component.translatable("player-migration.rename-error", oldNickname));
      LOGGER.error("Error while saving player migration", e);
      return -1;
    }
    transferScoreboardScores(ctx.getSource().getServer(), oldNickname, newNickname);
    if (successful) {
      LOGGER.info("Player '{}' has been renamed to '{}'", oldNickname, newNickname);
      ctx.getSource().sendSuccess(() ->
              Component.translatable("player-migration.rename-success", oldNickname, newNickname),
          true);
    } else {
      ctx.getSource().sendSuccess(
          () -> Component.translatable("player-migration.rename-exists"), false);
    }
    var player = ctx.getSource().getServer().getPlayerList().getPlayerByName(oldNickname);
    if (player != null) {

      // todo disconnect and whitelist check
      player.sendSystemMessage(
          Component.translatable("player-migration.renamed-player-notification", newNickname));
    }
    return 0;
  }

  private static void transferScoreboardScores(
      MinecraftServer server, String oldName, String newName) {
    var scoreboard = server.getScoreboard();
    var oldHolder = ScoreHolder.forNameOnly(oldName);
    var newHolder = ScoreHolder.forNameOnly(newName);
    for (var entry : scoreboard.listPlayerScores(oldHolder).object2IntEntrySet()) {
      var objective = entry.getKey();
      int score = entry.getIntValue();
      scoreboard.getOrCreatePlayerScore(newHolder, objective, true).set(score);
    }
    scoreboard.resetAllPlayerScores(oldHolder);
  }
}
