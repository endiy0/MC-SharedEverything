package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class TeamInventoryWatcher implements Runnable {
    private final SharedEverythingPlugin plugin;
    private final Map<UUID, String> lastTeams = new HashMap<>();

    TeamInventoryWatcher(SharedEverythingPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        boolean inventoryEnabled = plugin.isInventoryEnabled();
        boolean teamInventoryEnabled = plugin.isTeamInventoryEnabled();
        Iterator<Map.Entry<UUID, String>> iterator = lastTeams.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, String> entry = iterator.next();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                iterator.remove();
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            String teamName = getTeamName(player);
            String previous = lastTeams.put(player.getUniqueId(), teamName);
            if (!Objects.equals(previous, teamName) && inventoryEnabled && teamInventoryEnabled) {
                plugin.getInventoryManager().updateInventory(player, true, true);
            } else if (previous == null) {
                lastTeams.put(player.getUniqueId(), teamName);
            }
        }
    }

    private String getTeamName(Player player) {
        Team team = Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player);
        return team == null ? "" : team.getName();
    }
}
