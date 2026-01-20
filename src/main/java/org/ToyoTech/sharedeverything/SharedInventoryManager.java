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
    private final SharedArmor sharedArmor;
    private final SharedOffHand sharedOffHand;
    private final Map<String, SharedInventory> teamInventories;
    private final Map<String, SharedArmor> teamArmors;
    private final Map<String, SharedOffHand> teamOffHands;
    private final Map<UUID, NmsBridge.InventoryLists> personalInventories = new HashMap<>();
    private final Map<UUID, InventoryState> inventoryStates = new HashMap<>();
    private final Map<UUID, String> teamAssignments = new HashMap<>();

    SharedInventoryManager(NmsBridge nms,
                           SharedInventory sharedInventory, SharedArmor sharedArmor, SharedOffHand sharedOffHand,
                           Map<String, SharedInventory> teamInventories, Map<String, SharedArmor> teamArmors, Map<String, SharedOffHand> teamOffHands) {
        this.nms = nms;
        this.sharedInventory = sharedInventory;
        this.sharedArmor = sharedArmor;
        this.sharedOffHand = sharedOffHand;
        this.teamInventories = teamInventories;
        this.teamArmors = teamArmors;
        this.teamOffHands = teamOffHands;
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
        nms.setInventoryLists(playerInventory, player, sharedInventory.getItemsList(), sharedArmor.getList(), sharedOffHand.getList());

        inventoryStates.put(player.getUniqueId(), InventoryState.SHARED);
        teamAssignments.remove(player.getUniqueId());
        player.updateInventory();
    }

    void handlePlayerDeath(Player player) {
        // NMS linking handles clearing shared inventory on death if not kept.
    }

    void applyTeamInventory(Player player, String teamName) {
        UUID playerId = player.getUniqueId();
        if (getState(player) == InventoryState.TEAM && teamName.equals(teamAssignments.get(playerId))) {
            return;
        }
        savePersonalInventory(player);
        
        SharedInventory teamInventory = teamInventories.computeIfAbsent(teamName, name -> new SharedInventory(nms));
        
        // Ensure team armor and offhand are created together if sharing map
        if (!teamArmors.containsKey(teamName) || !teamOffHands.containsKey(teamName)) {
            if (nms.isUsingEquipmentMap()) {
                Object sharedMap = nms.createEquipmentMap();
                teamArmors.put(teamName, new SharedArmor(nms, sharedMap));
                teamOffHands.put(teamName, new SharedOffHand(nms, sharedMap));
            } else {
                teamArmors.computeIfAbsent(teamName, name -> new SharedArmor(nms));
                teamOffHands.computeIfAbsent(teamName, name -> new SharedOffHand(nms));
            }
        }
        
        SharedArmor teamArmor = teamArmors.get(teamName);
        SharedOffHand teamOffHand = teamOffHands.get(teamName);

        Object playerInventory = nms.getPlayerInventory(player);
        nms.setInventoryLists(playerInventory, player, teamInventory.getItemsList(), teamArmor.getList(), teamOffHand.getList());

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
            nms.setInventoryLists(playerInventory, player, saved.items(), saved.armor(), saved.offhand());
            player.updateInventory();
        }
        inventoryStates.put(playerId, InventoryState.PERSONAL);
        teamAssignments.remove(playerId);
    }

    void refreshViewers(Player player) {
        // No manual sync needed with NMS linking
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

    private void savePersonalInventory(Player player) {
        UUID playerId = player.getUniqueId();
        if (personalInventories.containsKey(playerId)) {
            return;
        }
        Object playerInventory = nms.getPlayerInventory(player);
        NmsBridge.InventoryLists lists = nms.getInventoryLists(playerInventory, player);
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
