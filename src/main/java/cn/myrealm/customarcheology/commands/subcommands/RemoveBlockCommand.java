package cn.myrealm.customarcheology.commands.subcommands;

import cn.myrealm.customarcheology.commands.SubCommand;
import cn.myrealm.customarcheology.enums.Messages;
import cn.myrealm.customarcheology.enums.Permissions;
import cn.myrealm.customarcheology.managers.managers.ChunkManager;
import cn.myrealm.customarcheology.managers.managers.RemoveWorldAction;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author rzt1020
 */
public class RemoveBlockCommand implements SubCommand {
    private static final int REMOVE_WORLD_ARGUMENTS = 2;
    private static final int REMOVE_LOCATION_ARGUMENTS = 5;

    @Override
    public String getName() {
        return "removeblock";
    }

    @Override
    public String getDescription() {
        return Messages.COMMAND_REMOVEBLOCK.getMessage();
    }

    @Override
    public String getUsage() {
        return "/customarcheology removeblock <world> [x] [y] [z]";
    }

    @Override
    public List<String> getSubCommandAliases() {
        return new ArrayList<>();
    }

    @Override
    public List<String> onTabComplete(int argsNum, String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (argsNum == FIRST_ARGUMENT) {
            suggestions.addAll(Bukkit.getWorlds().stream().map(World::getName).toList());
        } else if (argsNum == SECOND_ARGUMENT) {
            suggestions = List.of("[x]");
        } else if (argsNum == THIRD_ARGUMENT) {
            suggestions = List.of("[y]");
        } else if (argsNum == FOURTH_ARGUMENT) {
            suggestions = List.of("[z]");
        }
        return suggestions;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!Permissions.COMMAND_REMOVEBLOCK.hasPermission(sender)) {
            return;
        }

        if (args.length != REMOVE_WORLD_ARGUMENTS && args.length != REMOVE_LOCATION_ARGUMENTS) {
            sender.sendMessage(Messages.ERROR_INCORRECT_COMMAND.getMessageWithPrefix());
            return;
        }

        World world = Bukkit.getWorld(args[FIRST_ARGUMENT]);
        if (Objects.isNull(world)) {
            sender.sendMessage(Messages.ERROR_WORLD_NOT_FOUND.getMessageWithPrefix());
            return;
        }

        ChunkManager chunkManager = ChunkManager.getInstance();
        if (args.length == REMOVE_WORLD_ARGUMENTS) {
            RemoveWorldAction action = chunkManager.toggleRemoveManagedBlocks(
                    world,
                    (processedChunks, totalChunks) -> notifySenderAndConsole(
                            sender,
                            Messages.PREFIX.getMessage() + "Removeblock task progress in world " + world.getName() + ": "
                                    + Math.min(100, (processedChunks * 100) / Math.max(1, totalChunks))
                                    + "% (" + processedChunks + "/" + totalChunks + " chunks)."
                    ),
                    removedBlocks ->
                    notifySenderAndConsole(sender, Messages.GAME_REMOVE_WORLD_BLOCKS.getMessageWithPrefix(
                            "world", world.getName(),
                            "amount", String.valueOf(removedBlocks)
                    ))
            );
            if (action == RemoveWorldAction.CANCELED) {
                notifySenderAndConsole(sender, Messages.PREFIX.getMessage() + "Canceled removeblock task for world " + world.getName() + ".");
            } else {
                notifySenderAndConsole(sender, Messages.PREFIX.getMessage() + "Removeblock task is running in world " + world.getName() + ".");
            }
            return;
        }

        Location location = new Location(
                world,
                Integer.parseInt(args[SECOND_ARGUMENT]),
                Integer.parseInt(args[THIRD_ARGUMENT]),
                Integer.parseInt(args[FOURTH_ARGUMENT])
        );
        if (!chunkManager.removeManagedBlock(location)) {
            sender.sendMessage(Messages.ERROR_ARCHEOLOGY_BLOCK_NOT_FOUND.getMessageWithPrefix(
                    "world", world.getName(),
                    "pos", args[SECOND_ARGUMENT] + ", " + args[THIRD_ARGUMENT] + ", " + args[FOURTH_ARGUMENT]
            ));
            return;
        }

        sender.sendMessage(Messages.GAME_REMOVE_BLOCK.getMessageWithPrefix(
                "world", world.getName(),
                "pos", args[SECOND_ARGUMENT] + ", " + args[THIRD_ARGUMENT] + ", " + args[FOURTH_ARGUMENT]
        ));
    }

    private void notifySenderAndConsole(CommandSender sender, String message) {
        sender.sendMessage(message);
        if (sender != Bukkit.getConsoleSender()) {
            Bukkit.getConsoleSender().sendMessage(message);
        }
    }
}
