package com.joyxz.commands;

import com.joyxz.WorldConfig;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.io.File;

public class WorldLoaderCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("wl")
                        .requires(source -> source.isPlayer())
                        .then(
                                Commands.literal("list")
                                        .executes(context -> {
                                            File serverDir = new File(".", "worlds");

                                            if (!serverDir.exists()) {
                                                context.getSource().sendSuccess(
                                                        () -> Component.literal("No worlds directory found."), false
                                                );
                                                return 0;
                                            }

                                            File[] folders = serverDir.listFiles(File::isDirectory);

                                            if (folders == null) {
                                                context.getSource().sendSuccess(
                                                        () -> Component.literal("Error: could not read worlds directory"), false
                                                );
                                                return 0;
                                            }

                                            StringBuilder msg = new StringBuilder("Worlds Found: \n");
                                            for (File folder : folders) {
                                                if (new File(folder, "level.dat").exists()) {
                                                    msg.append("- ").append(folder.getName()).append("\n");
                                                }
                                            }

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(msg.toString()), false
                                            );

                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("load")
                                        .then(
                                                Commands.argument("worldName", StringArgumentType.greedyString())
                                                        .suggests((context, builder) -> {
                                                            File worldsDir = new File(".", "worlds");
                                                            File[] folders = worldsDir.listFiles(File::isDirectory);
                                                            if (folders != null) {
                                                                for (File folder : folders) {
                                                                    if (new File(folder, "level.dat").exists()) {
                                                                        builder.suggest(folder.getName());
                                                                    }
                                                                }
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(context -> {
                                                            String worldName = StringArgumentType.getString(context, "worldName");
                                                            net.minecraft.server.MinecraftServer server = context.getSource().getServer();

                                                            File worldFolder = new File(".", "worlds/" + worldName);
                                                            if (!worldFolder.exists() || !new File(worldFolder, "level.dat").exists()) {
                                                                context.getSource().sendSuccess(
                                                                        () -> Component.literal("World '" + worldName + "' not found or invalid."), false
                                                                );
                                                                return 0;
                                                            }

                                                            File source = new File(".", "worlds/" + worldName);
                                                            File target = new File(".", "world/dimensions/minecraft/" + worldName);

                                                            if (!target.exists()) {
                                                                try {
                                                                    copyFolder(source.toPath(), target.toPath());
                                                                    context.getSource().sendSuccess(
                                                                            () -> Component.literal("Copied world data..."), false
                                                                    );
                                                                } catch (Exception e) {
                                                                    context.getSource().sendSuccess(
                                                                            () -> Component.literal("Failed to copy world data: " + e.getMessage()), false
                                                                    );
                                                                    return 0;
                                                                }
                                                            }

                                                            Fantasy fantasy = Fantasy.get(server);
                                                            RuntimeWorldConfig config = new RuntimeWorldConfig()
                                                                    .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
                                                                    .setGenerator(server.getLevel(Level.OVERWORLD).getChunkSource().getGenerator())
                                                                    .setSeed(server.getLevel(Level.OVERWORLD).getSeed());

                                                            fantasy.getOrOpenPersistentWorld(Identifier.parse(worldName), config);
                                                            WorldConfig.addWorld(worldName);

                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal("World '" + worldName + "' loaded!"), false
                                                            );

                                                            return 1;
                                                        })
                                        )
                        )
                        .then(
                                Commands.literal("tp")
                                        .then(
                                                Commands.argument("worldName", StringArgumentType.greedyString())
                                                        .suggests((context, builder) -> {
                                                            for (ServerLevel l : context.getSource().getServer().getAllLevels()) {
                                                                String key = l.dimension().toString();
                                                                if (!key.contains("overworld") && !key.contains("the_nether") && !key.contains("the_end")) {
                                                                    builder.suggest(l.dimension().toString()
                                                                            .replaceAll("ResourceKey\\[minecraft:dimension / minecraft:", "")
                                                                            .replace("]", ""));
                                                                }
                                                            }
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(context -> {
                                                            String worldName = StringArgumentType.getString(context, "worldName");
                                                            MinecraftServer server = context.getSource().getServer();

                                                            ServerLevel level = null;
                                                            for (ServerLevel l : server.getAllLevels()) {
                                                                if (l.dimension().toString().equals("ResourceKey[minecraft:dimension / minecraft:" + worldName + "]")) {
                                                                    level = l;
                                                                    break;
                                                                }
                                                            }

                                                            if (level == null) {
                                                                context.getSource().sendSuccess(
                                                                        () -> Component.literal("World '" + worldName + "' is not loaded, use /wl load first."), false
                                                                );
                                                                return 0;
                                                            }

                                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                                            player.teleportTo(level, 0.0, 64.0, 0.0, java.util.Set.of(), player.getYRot(), player.getXRot(), true);

                                                            context.getSource().sendSuccess(
                                                                    () -> Component.literal("Teleporting to '" + worldName + "'."), false
                                                            );

                                                            return 1;
                                                        })
                                        )
                        )
                        .then(
                                Commands.literal("debug")
                                        .executes(context -> {
                                            for (ServerLevel l : context.getSource().getServer().getAllLevels()) {
                                                context.getSource().sendSuccess(
                                                        () -> Component.literal("Level: " + l.dimension()), false
                                                );
                                            }
                                            return 1;
                                        })
                        )
        );
    }

    // Checks if dimension path exists and if not, creates it and copies the files.
    public static void copyFolder(java.nio.file.Path source, java.nio.file.Path target) throws java.io.IOException {
        java.nio.file.Files.createDirectories(target);

        try (var stream = java.nio.file.Files.walk(source)) {
            stream.forEach(src -> {
                try {
                    java.nio.file.Files.copy(src, target.resolve(source.relativize(src)),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.io.IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }
}