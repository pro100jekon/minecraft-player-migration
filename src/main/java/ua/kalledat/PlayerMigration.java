package ua.kalledat;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerMigration implements ModInitializer {

    public static final String MOD_ID = "player-migration";

    public static JsonFileRepository jsonFileRepository;
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            jsonFileRepository = new JsonFileRepository(server.getPath("player-nickname-migrations/migrations.json"));
        });
        CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) ->
                        dispatcher.register(createTransferPlayerCommand()));
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createTransferPlayerCommand() {
        return literal("transferplayer")
                .requires(Permissions.require("player_migration.command.transferplayer", 4))
                .then(argument("old_nickname", StringArgumentType.word())
                        .then(argument("new_nickname", StringArgumentType.word())
                                .executes(createPlayerMigration())));
    }

    private static Command<ServerCommandSource> createPlayerMigration() {
        return ctx -> {
            var oldNickname = StringArgumentType.getString(ctx, "old_nickname");
            var newNickname = StringArgumentType.getString(ctx, "new_nickname");
            try {
                jsonFileRepository.saveNewPlayerMigration(Map.entry(oldNickname, newNickname));
            } catch (IOException e) {
                ctx.getSource().sendFeedback(() ->
                        Text.literal("Помилка при створенні міграції нікнейму гравця"), true);
                LOGGER.error("Error while saving player migration", e);
                return -1;
            }
            LOGGER.info("{} has been transferred to {}", oldNickname, newNickname);
            ctx.getSource().sendFeedback(() ->
                    Text.literal(oldNickname + " -> " + newNickname), true);
            return 0;
        };
    }
}