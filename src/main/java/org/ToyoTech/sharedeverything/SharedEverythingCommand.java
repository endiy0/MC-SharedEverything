package org.ToyoTech.sharedeverything;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

final class SharedEverythingCommand implements CommandExecutor, TabCompleter {
    private final SharedEverythingPlugin plugin;

    SharedEverythingCommand(SharedEverythingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("sharedeverything.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                plugin.reloadPlugin();
                sender.sendMessage(ChatColor.GREEN + "SharedEverything reloaded.");
            }
            case "reset" -> handleReset(sender, args);
            case "status" -> plugin.sendStatus(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }
        String target = args[1].toLowerCase(Locale.ROOT);
        switch (target) {
            case "inventory" -> {
                plugin.resetGlobalInventory();
                sender.sendMessage(ChatColor.GREEN + "Global inventory reset.");
            }
            case "advancements" -> {
                plugin.resetGlobalAdvancements();
                sender.sendMessage(ChatColor.GREEN + "Global advancements reset.");
            }
            case "all" -> {
                plugin.resetGlobalInventory();
                plugin.resetGlobalAdvancements();
                sender.sendMessage(ChatColor.GREEN + "Global inventory and advancements reset.");
            }
            default -> sendUsage(sender);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything reload");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything reset <inventory|advancements|all>");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything status");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList("reload", "reset", "status"), args[0]);
        }
        if (args.length == 2 && "reset".equalsIgnoreCase(args[0])) {
            return filter(Arrays.asList("inventory", "advancements", "all"), args[1]);
        }
        return List.of();
    }

    private List<String> filter(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }
}

