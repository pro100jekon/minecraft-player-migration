package ua.kalledat.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static ua.kalledat.PlayerMigration.*;

public class TransferPlayerCommand {

    public static void registerCommand() {
        CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) ->
                        dispatcher.register(createTransferPlayerCommand()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createTransferPlayerCommand() {
        return literal("transferplayer")
                .requires(Permissions.require(MOD_ID + ".command.transferplayer", 4))
                .then(argument("old_nickname", StringArgumentType.word())
                        .then(argument("new_nickname", StringArgumentType.word())
                                .executes(createPlayerMigration())));
    }

    private static Command<ServerCommandSource> createPlayerMigration() {
        return ctx -> {
            var oldNickname = StringArgumentType.getString(ctx, "old_nickname");
            var newNickname = StringArgumentType.getString(ctx, "new_nickname");
            try {
                playerMigrationRepo.saveNewPlayerMigration(Map.entry(oldNickname, newNickname));
            } catch (IOException e) {
                ctx.getSource().sendFeedback(() ->
                        Text.translatable("player-migration.rename-error", oldNickname), true);
                LOGGER.error("Error while saving player migration", e);
                return -1;
            }
            LOGGER.info("Player '{}' has been renamed to '{}'", oldNickname, newNickname);
            ctx.getSource().sendFeedback(() ->
                    Text.translatable("player-migration.rename-success",oldNickname, newNickname), true);
            var player = ctx.getSource().getServer().getPlayerManager().getPlayer(oldNickname);
            if (player != null) {
                player.sendMessage(Text.translatable("player-migration.renamed-player-notification", newNickname));
            }
            return 0;
        };
    }
}
