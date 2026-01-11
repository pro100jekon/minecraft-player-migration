package ua.kalledat.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static ua.kalledat.PlayerMigration.LOGGER;
import static ua.kalledat.PlayerMigration.MOD_ID;
import static ua.kalledat.PlayerMigration.playerMigrationRepo;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import java.io.IOException;
import java.util.Map;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;

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
    var currentNickname = playerMigrationRepo.getCurrentNickname(oldNickname).orElse(oldNickname);
    var successful = false;
    try {
      successful = playerMigrationRepo.saveNewPlayerMigration(Map.entry(oldNickname, newNickname));
    } catch (IOException e) {
      ctx.getSource().sendFailure(
          Component.translatable("player-migration.rename-error", oldNickname));
      LOGGER.error("Error while saving player migration", e);
      return -1;
    }
    var uuid = UUIDUtil.createOfflinePlayerUUID(newNickname);
    transferScoreboardScores(ctx.getSource().getServer(),
        new GameProfile(uuid, oldNickname), new GameProfile(uuid, newNickname));
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
      player.connection.disconnect(
          Component.literal("You were renamed to '%s'. Please login with this nickname"
              .formatted(newNickname)));
    }
    var wl = player.level().getServer().getPlayerList().getWhiteList();
    wl.remove(new NameAndId(uuid, currentNickname));
    wl.add(new UserWhiteListEntry(
        new NameAndId(uuid, newNickname)));
    return 0;
  }

  private static void transferScoreboardScores(
      MinecraftServer server, GameProfile oldName, GameProfile newName) {
    var scoreboard = (Scoreboard) server.getScoreboard();
    var removed = scoreboard.playerScores.remove(oldName.name());
    var oldHolder = ScoreHolder.fromGameProfile(oldName);
    if (removed != null) {
      scoreboard.onPlayerRemoved(oldHolder);
      scoreboard.playerScores.put(newName.name(), removed);
    }
  }
}
