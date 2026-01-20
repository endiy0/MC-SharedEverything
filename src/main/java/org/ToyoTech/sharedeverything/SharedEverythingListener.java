package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldSaveEvent;

final class SharedEverythingListener implements Listener {
    private final SharedEverythingPlugin plugin;

    SharedEverythingListener(SharedEverythingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getInventoryManager().updateInventory(player, plugin.isInventoryEnabled(), plugin.isTeamInventoryEnabled());
        if (plugin.isAdvancementEnabled()) {
            plugin.getAdvancementShareManager().applyToPlayer(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getInventoryManager().updateInventory(player, plugin.isInventoryEnabled(), plugin.isTeamInventoryEnabled());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getInventoryManager().applyPersonalInventory(player);
        plugin.getInventoryManager().clearPlayerState(player);
        plugin.saveSharedData(false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldSave(WorldSaveEvent event) {
        plugin.saveSharedData(false);
        if (!plugin.isInventoryEnabled()) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.getInventoryManager().applyPersonalInventory(player);
            player.saveData();
            plugin.getInventoryManager().updateInventory(player, plugin.isInventoryEnabled(), plugin.isTeamInventoryEnabled());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        if (!plugin.isAdvancementEnabled()) {
            return;
        }
        plugin.getAdvancementShareManager().recordAdvancement(event.getPlayer(), event.getAdvancement());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (plugin.isInventoryEnabled()) {
            if (plugin.shouldKeepInventoryOnDeath()) {
                event.setKeepInventory(true);
                event.getDrops().clear();
            } else {
                plugin.getInventoryManager().handlePlayerDeath(event.getEntity());
            }
        }
        if (plugin.isAnnounceDeathEnabled()) {
            Location location = event.getEntity().getLocation();
            World world = location.getWorld();
            String worldName = world == null ? "unknown" : world.getName();
            String message = ChatColor.YELLOW + event.getEntity().getName() + " died at "
                    + worldName + " (" + location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ() + ")";
            Bukkit.broadcastMessage(message);
        }
    }
}