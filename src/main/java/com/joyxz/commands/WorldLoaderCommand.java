package com.joyxz.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.io.File;

public class WorldLoaderCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("wl")
                        .requires(source -> source.isPlayer())
                        .then(
                                Commands.literal("list")
                                        .executes(context -> {
                                            // get server instance
                                            net.minecraft.server.MinecraftServer server = context.getSource().getServer();

                                            //get server root directory
                                            File serverDir = server.getServerDirectory().toFile();

                                            // Get all folders in server root
                                            File[] folders = serverDir.listFiles(File::isDirectory);
                                            assert folders != null;

                                            StringBuilder msg = new StringBuilder("Worlds Found: \n");

                                            for (File folder : folders) {
                                                // Check if world file is valid
                                                if (new File(folder, "level.dat").exists()) {
                                                    msg.append("- ").append(folder.getName()).append("\n");

                                                }
                                            }

                                            // Send message to player who ran the command
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(msg.toString()), false
                                            );

                                            return 1;
                                        })
                        )
        );
    }

}
