package org.ToyoTech.sharedeverything;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

final class InventorySyncListener implements Listener {
    private final SharedEverythingPlugin plugin;

    InventorySyncListener(SharedEverythingPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled()) {
            return;
        }
        plugin.runNextTick(() -> plugin.applyGlobalInventoryToPlayer(player));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onItemBreak(PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.hasItem()) {
            return;
        }
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        Material type = event.getItem().getType();
        if (isArmorItem(type)) {
            plugin.scheduleInventorySnapshot(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.isInventorySyncEnabled()) {
            return;
        }
        if (plugin.shouldKeepInventoryOnDeath()) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            return;
        }
        plugin.runNextTick(() -> plugin.captureAndBroadcastInventory(event.getEntity()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled()) {
            return;
        }
        plugin.runNextTick(() -> plugin.applyGlobalInventoryToPlayer(player));
    }

    private boolean isArmorItem(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.equals("ELYTRA");
    }
}

