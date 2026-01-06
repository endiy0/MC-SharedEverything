package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.WorldSaveEvent;

final class SharedEverythingListener implements Listener {
    private final SharedEverythingPlugin plugin;

    SharedEverythingListener(SharedEverythingPlugin plugin) {
        this.plugin = plugin;
    }

    private void delayedRefresh(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getInventoryManager().refreshViewers(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            delayedRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            delayedRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            delayedRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        delayedRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        delayedRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        delayedRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        delayedRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerItemBreak(PlayerItemBreakEvent event) {
        delayedRefresh(event.getPlayer());
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
        if (plugin.isInventoryEnabled() && plugin.shouldKeepInventoryOnDeath()) {
            event.setKeepInventory(true);
            event.getDrops().clear();
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
