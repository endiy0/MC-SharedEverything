package org.ToyoTech.sharedeverything;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
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
    public void onInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        Player player = event.getPlayer();
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
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
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
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        if (event.getPlayer().getInventory().getItem(event.getHand()) == null) {
            return;
        }
        plugin.scheduleInventorySnapshot(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isInventorySyncEnabled() || plugin.isSyncing(player)) {
            return;
        }
        if (event.getPlayer().getInventory().getItem(event.getHand()) == null) {
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
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
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

}

