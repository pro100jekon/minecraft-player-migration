package ua.kalledat;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static ua.kalledat.PlayerMigration.MOD_ID;

public class JsonFileRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path filePath;

    private final BiMap<String, String> migrations = HashBiMap.create();

    public JsonFileRepository(Path filePath) {
        this.filePath = filePath;
        load();
    }

    public void saveNewPlayerMigration(Map.Entry<String, String> migration) {
        if (Objects.equals(migrations.inverse().get(migration.getKey()), migration.getValue())) {
            // Have entry nickname1 -> nickname2 and receive nickname2 -> nickname1 => remove migration
            migrations.remove(migration.getValue());
        } else if (migrations.inverse().containsKey(migration.getKey())) {
            // Have entry nickname1 -> nickname2 and receive nickname2 -> nickname3 => make nickname1 -> nickname3
            migrations.put(migrations.inverse().get(migration.getKey()), migration.getValue());
        } else {
            // Have entry nickname1 -> nickname2 and receive nickname1 -> nickname3 => make nickname1 -> nickname3
            // Also covers adding new migrations
            migrations.put(migration.getKey(), migration.getValue());
        }
        save();
    }

    public Optional<String> getOriginalNickname(String nickname) {
        return Optional.ofNullable(migrations.inverse().get(nickname));
    }

    private void load() {
        try {
            if (Files.exists(filePath)) {
                var content = Files.readString(filePath);
                var parsed = GSON.fromJson(content, new TypeToken<Map<String, String>>() {
                });
                if (parsed != null) migrations.putAll(parsed);
            } else {
                Files.createDirectories(filePath.getParent());
                save();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON data at " + filePath, e);
        }
    }

    private void save() {
        try {
            Files.writeString(filePath, GSON.toJson(migrations));
        } catch (IOException e) {
            LOGGER.error("Failed to save JSON data at {}", filePath, e);
        }
    }
}
