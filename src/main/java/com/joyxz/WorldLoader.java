package com.joyxz;

import com.joyxz.commands.WorldLoaderCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.Level;
import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

public class WorldLoader implements ModInitializer {
    public static final String MOD_ID = "world-loader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private void autoLoadWorlds(net.minecraft.server.MinecraftServer server) {
        List<String> worlds = WorldConfig.getAutoLoadWorlds();

        for (String worldName : worlds) {
            try {
                File source = new File(".", "worlds/" + worldName);
                File target = new File(".", "world/dimensions/minecraft/" + worldName);

                if (!target.exists()) {
                    WorldLoader.LOGGER.info("Copying world data for: " + worldName);
                    WorldLoaderCommand.copyFolder(source.toPath(), target.toPath());
                }

                Fantasy fantasy = Fantasy.get(server);
                RuntimeWorldConfig config = new RuntimeWorldConfig()
                        .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                        .setGenerator(server.getLevel(Level.OVERWORLD).getChunkSource().getGenerator())
                        .setSeed(server.getLevel(Level.OVERWORLD).getSeed());

                fantasy.getOrOpenPersistentWorld(Identifier.parse(worldName), config);
                WorldLoader.LOGGER.info("Auto-loaded world: " + worldName);
            } catch (Exception e) {
                WorldLoader.LOGGER.error("Failed to auto-load world " + worldName + ": " + e.getMessage());
            }
        }
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(this::autoLoadWorlds);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            WorldLoaderCommand.register(dispatcher);
        });

        WorldConfig.init();
    }
}