package com.joyxz;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class WorldConfig {

    private static final Path CONFIG_FILE = Path.of(".", "config", "worldloader.json");

    // Creates the config file with empty autoload list if it doesn't exist
    public static void init() {
        if (!Files.exists(CONFIG_FILE)) {
            save(new ArrayList<>());
            WorldLoader.LOGGER.info("Created default worldloader.json config");
        }
    }

    public static List<String> getAutoLoadWorlds() {
        List<String> worlds = new ArrayList<>();
        if (!Files.exists(CONFIG_FILE)) return worlds;

        try {
            JsonObject json = JsonParser.parseReader(
                    new FileReader(CONFIG_FILE.toFile())
            ).getAsJsonObject();

            JsonArray array = json.getAsJsonArray("autoload");
            for (JsonElement element : array) {
                worlds.add(element.getAsString());
            }
        } catch (Exception e) {
            WorldLoader.LOGGER.error("Failed to read world config: " + e.getMessage());
        }

        return worlds;
    }

    public static void addWorld(String worldName) {
        List<String> worlds = getAutoLoadWorlds();
        if (!worlds.contains(worldName)) {
            worlds.add(worldName);
            save(worlds);
        }
    }

    public static void removeWorld(String worldName) {
        List<String> worlds = getAutoLoadWorlds();
        worlds.remove(worldName);
        save(worlds);
    }

    private static void save(List<String> worlds) {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());

            JsonObject json = new JsonObject();
            JsonArray array = new JsonArray();
            worlds.forEach(array::add);
            json.add("autoload", array);

            try (FileWriter writer = new FileWriter(CONFIG_FILE.toFile())) {
                new GsonBuilder().setPrettyPrinting().create().toJson(json, writer);
            }
        } catch (Exception e) {
            WorldLoader.LOGGER.error("Failed to save world config: " + e.getMessage());
        }
    }
}