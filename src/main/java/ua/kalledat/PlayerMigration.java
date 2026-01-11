package ua.kalledat;

import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import java.io.IOException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.kalledat.command.TransferPlayerCommand;

public class PlayerMigration implements ModInitializer {

  public static final String MOD_ID = "player-migration";

  public static JsonFileRepository playerMigrationRepo;
  public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

  private static MinecraftServer minecraftServer;

  @Override
  public void onInitialize() {
    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      minecraftServer = server;
      if (!server.usesAuthentication()) {
        playerMigrationRepo = new JsonFileRepository(
            server.getFile("player-nickname-migrations/migrations.json"));
      }
    });
    if (minecraftServer != null && minecraftServer.usesAuthentication()) {
      LOGGER.info(
          "Player migration feature is turned off, since the server is started in online mode!");
      return;
    }
    ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
      try {
        playerMigrationRepo.save();
      } catch (IOException ignored) {

      }
    });
    TransferPlayerCommand.registerCommand();
    PolymerResourcePackUtils.addModAssets(MOD_ID);
    PolymerResourcePackUtils.markAsRequired();
  }
}