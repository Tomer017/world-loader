package com.joyxz;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.UUID;

public class PlayerPositions {

    private static final Path POSITIONS_FILE = Path.of(".", "config", "worldloader-positions.json");

    private static JsonObject load() {
        if (!Files.exists(POSITIONS_FILE)) return new JsonObject();

        try {
            return JsonParser.parseReader(new FileReader(POSITIONS_FILE.toFile())).getAsJsonObject();
        } catch (Exception e) {
            WorldLoader.LOGGER.error("Failed to read positions file: " + e.getMessage());
            return new JsonObject();
        }
    }

    private static void save(JsonObject data) {
        try {
            Files.createDirectories(POSITIONS_FILE.getParent());
            try (FileWriter writer = new FileWriter(POSITIONS_FILE.toFile())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(data, writer);
            }
        } catch (Exception e) {
            WorldLoader.LOGGER.error("Failed to save positions file: {}", e.getMessage());
        }
    }

    public static void savePosition(UUID playerUUID, String worldName, double x, double y, double z, float yRot, float xRot) {
        JsonObject data = load();
        String key = playerUUID.toString() + "_" + worldName;

        JsonObject pos = new JsonObject();
        pos.addProperty("x", x);
        pos.addProperty("y", y);
        pos.addProperty("z", z);
        pos.addProperty("yRot", yRot);
        pos.addProperty("xRot", xRot);

        data.add(key, pos);
        save(data);
    }

    public static double[] getPosition(UUID playerUUID, String worldName) {
        JsonObject data = load();
        String key = playerUUID.toString() + "_" + worldName;

        if (!data.has(key)) return null;

        JsonObject pos = data.getAsJsonObject(key);
        return new double[]{
                pos.get("x").getAsDouble(),
                pos.get("y").getAsDouble(),
                pos.get("z").getAsDouble(),
                pos.get("yRot").getAsFloat(),
                pos.get("xRot").getAsFloat()

        };
    }


}
