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
            case "inventory" -> handleToggle(sender, args, "inventory");
            case "advancement" -> handleToggle(sender, args, "advancement");
            case "announcedeath" -> handleToggle(sender, args, "announcedeath");
            case "teaminventory" -> handleToggle(sender, args, "teaminventory");
            case "keepinventory" -> handleToggle(sender, args, "keepinventory");
            case "reload" -> plugin.reloadPlugin(sender);
            case "reset" -> handleReset(sender, args);
            case "status" -> plugin.sendStatus(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleToggle(CommandSender sender, String[] args, String target) {
        if (args.length != 2) {
            sender.sendMessage(ChatColor.YELLOW + "Current " + target + ": " + getCurrentToggle(target));
            return;
        }
        Boolean value = parseBoolean(args[1]);
        if (value == null) {
            sendUsage(sender);
            return;
        }
        switch (target) {
            case "inventory" -> plugin.setInventoryEnabled(value);
            case "advancement" -> plugin.setAdvancementEnabled(value);
            case "announcedeath" -> plugin.setAnnounceDeathEnabled(value);
            case "teaminventory" -> plugin.setTeamInventoryEnabled(value);
            case "keepinventory" -> plugin.setKeepInventoryOnDeath(value);
            default -> {
                sendUsage(sender);
                return;
            }
        }
        sender.sendMessage(ChatColor.GREEN + target + " set to " + value);
    }

    private String getCurrentToggle(String target) {
        return switch (target) {
            case "inventory" -> String.valueOf(plugin.isInventoryEnabled());
            case "advancement" -> String.valueOf(plugin.isAdvancementEnabled());
            case "announcedeath" -> String.valueOf(plugin.isAnnounceDeathEnabled());
            case "teaminventory" -> String.valueOf(plugin.isTeamInventoryEnabled());
            case "keepinventory" -> String.valueOf(plugin.shouldKeepInventoryOnDeath());
            default -> "unknown";
        };
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendUsage(sender);
            return;
        }
        String target = args[1].toLowerCase(Locale.ROOT);
        switch (target) {
            case "inventory" -> {
                plugin.resetSharedInventory();
                sender.sendMessage(ChatColor.GREEN + "Shared inventory reset.");
            }
            case "advancements" -> {
                plugin.resetAdvancements();
                sender.sendMessage(ChatColor.GREEN + "Shared advancements reset.");
            }
            case "all" -> {
                plugin.resetSharedInventory();
                plugin.resetAdvancements();
                sender.sendMessage(ChatColor.GREEN + "Shared inventory and advancements reset.");
            }
            default -> sendUsage(sender);
        }
    }

    private Boolean parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return null;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ChatColor.YELLOW + "Usage:");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything inventory <true|false>");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything advancement <true|false>");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything announcedeath <true|false>");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything teaminventory <true|false>");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything keepinventory <true|false>");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything reset <inventory|advancements|all>");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything reload");
        sender.sendMessage(ChatColor.YELLOW + "/sharedeverything status");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(Arrays.asList(
                    "inventory",
                    "advancement",
                    "announcedeath",
                    "teaminventory",
                    "keepinventory",
                    "reset",
                    "reload",
                    "status"
            ), args[0]);
        }
        if (args.length == 2) {
            if ("reset".equalsIgnoreCase(args[0])) {
                return filter(Arrays.asList("inventory", "advancements", "all"), args[1]);
            }
            return filter(Arrays.asList("true", "false"), args[1]);
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
