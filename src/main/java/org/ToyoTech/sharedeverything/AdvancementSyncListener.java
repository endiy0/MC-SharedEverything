package org.ToyoTech.sharedeverything;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;

final class AdvancementSyncListener implements Listener {
    private final SharedEverythingPlugin plugin;
    private final AdvancementSyncManager advancementSyncManager;

    AdvancementSyncListener(SharedEverythingPlugin plugin, AdvancementSyncManager advancementSyncManager) {
        this.plugin = plugin;
        this.advancementSyncManager = advancementSyncManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        advancementSyncManager.handleAdvancementDone(event.getPlayer(), event.getAdvancement());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!plugin.isAdvancementSyncEnabled()) {
            return;
        }
        plugin.runNextTick(() -> advancementSyncManager.applyGlobalAdvancementsToPlayer(player));
    }
}

