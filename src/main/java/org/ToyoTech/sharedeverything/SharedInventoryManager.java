package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class SharedInventoryManager {
    private final NmsBridge nms;
    private final SharedInventory sharedInventory;
    private final Map<String, SharedInventory> teamInventories;
    private final Map<UUID, NmsBridge.InventoryLists> personalInventories = new HashMap<>();
    private final Map<UUID, InventoryState> inventoryStates = new HashMap<>();
    private final Map<UUID, String> teamAssignments = new HashMap<>();

    SharedInventoryManager(NmsBridge nms, SharedInventory sharedInventory, Map<String, SharedInventory> teamInventories) {
        this.nms = nms;
        this.sharedInventory = sharedInventory;
        this.teamInventories = teamInventories;
    }

    void updateInventory(Player player, boolean inventoryEnabled, boolean teamInventoryEnabled) {
        if (!inventoryEnabled) {
            applyPersonalInventory(player);
            return;
        }
        if (teamInventoryEnabled) {
            Team team = Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player);
            if (team != null) {
                applyTeamInventory(player, team.getName());
                return;
            }
        }
        applySharedInventory(player);
    }

    void applySharedInventory(Player player) {
        if (getState(player) == InventoryState.SHARED) {
            return;
        }
        savePersonalInventory(player);
        Object playerInventory = nms.getPlayerInventory(player);
        nms.setInventoryLists(playerInventory, sharedInventory.getItemsList(), sharedInventory.getArmorList(), sharedInventory.getOffhandList());
        inventoryStates.put(player.getUniqueId(), InventoryState.SHARED);
        teamAssignments.remove(player.getUniqueId());
        player.updateInventory();
    }

    void applyTeamInventory(Player player, String teamName) {
        UUID playerId = player.getUniqueId();
        if (getState(player) == InventoryState.TEAM && teamName.equals(teamAssignments.get(playerId))) {
            return;
        }
        savePersonalInventory(player);
        SharedInventory teamInventory = teamInventories.computeIfAbsent(teamName, name -> new SharedInventory(nms));
        Object playerInventory = nms.getPlayerInventory(player);
        nms.setInventoryLists(playerInventory, teamInventory.getItemsList(), teamInventory.getArmorList(), teamInventory.getOffhandList());
        inventoryStates.put(playerId, InventoryState.TEAM);
        teamAssignments.put(playerId, teamName);
        player.updateInventory();
    }

    void applyPersonalInventory(Player player) {
        UUID playerId = player.getUniqueId();
        if (getState(player) == InventoryState.PERSONAL) {
            return;
        }
        NmsBridge.InventoryLists saved = personalInventories.remove(playerId);
        if (saved != null) {
            Object playerInventory = nms.getPlayerInventory(player);
            nms.setInventoryLists(playerInventory, saved.items(), saved.armor(), saved.offhand());
            player.updateInventory();
        }
        inventoryStates.put(playerId, InventoryState.PERSONAL);
        teamAssignments.remove(playerId);
    }

    void clearPlayerState(Player player) {
        UUID playerId = player.getUniqueId();
        personalInventories.remove(playerId);
        inventoryStates.remove(playerId);
        teamAssignments.remove(playerId);
    }

    void applyToAllPlayers(boolean inventoryEnabled, boolean teamInventoryEnabled) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateInventory(player, inventoryEnabled, teamInventoryEnabled);
        }
    }

    void resetSharedInventory() {
        sharedInventory.clear();
    }

    private void savePersonalInventory(Player player) {
        UUID playerId = player.getUniqueId();
        if (personalInventories.containsKey(playerId)) {
            return;
        }
        Object playerInventory = nms.getPlayerInventory(player);
        NmsBridge.InventoryLists lists = nms.getInventoryLists(playerInventory);
        personalInventories.put(playerId, lists);
        inventoryStates.put(playerId, InventoryState.PERSONAL);
    }

    private InventoryState getState(Player player) {
        return inventoryStates.getOrDefault(player.getUniqueId(), InventoryState.PERSONAL);
    }

    private enum InventoryState {
        PERSONAL,
        SHARED,
        TEAM
    }
}
