package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class AdvancementSyncManager {
    private static final int POLL_CHUNK_SIZE = 50;

    private final SharedEverythingPlugin plugin;
    private final Map<NamespacedKey, Set<String>> globalAdvancements;

    private boolean enabled;
    private int pollIntervalTicks;
    private List<String> excludedPrefixes = Collections.emptyList();
    private final List<Advancement> trackedAdvancements = new ArrayList<>();
    private int pollCursor;
    private BukkitTask pollTask;

    AdvancementSyncManager(SharedEverythingPlugin plugin, Map<NamespacedKey, Set<String>> globalAdvancements) {
        this.plugin = plugin;
        this.globalAdvancements = globalAdvancements;
    }

    void reload(boolean enabled, int pollIntervalTicks, List<String> excludedPrefixes) {
        this.enabled = enabled;
        this.pollIntervalTicks = Math.max(1, pollIntervalTicks);
        this.excludedPrefixes = excludedPrefixes == null ? Collections.emptyList() : new ArrayList<>(excludedPrefixes);
        rebuildAdvancementList();
    }

    void start() {
        stop();
        if (!enabled) {
            return;
        }
        pollTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::pollOnce, pollIntervalTicks, pollIntervalTicks);
    }

    void stop() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
    }

    void handleAdvancementDone(Player player, Advancement advancement) {
        if (!enabled || advancement == null) {
            return;
        }
        if (isExcluded(advancement.getKey())) {
            return;
        }
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        Set<String> awarded = new HashSet<>(progress.getAwardedCriteria());
        if (awarded.isEmpty()) {
            return;
        }
        Set<String> global = globalAdvancements.computeIfAbsent(advancement.getKey(), key -> new HashSet<>());
        boolean changed = global.addAll(awarded);
        if (changed) {
            awardMissingCriteriaToAll(advancement, global);
        }
    }

    void applyGlobalAdvancementsToPlayer(Player player) {
        if (!enabled) {
            return;
        }
        for (Map.Entry<NamespacedKey, Set<String>> entry : globalAdvancements.entrySet()) {
            Advancement advancement = Bukkit.getAdvancement(entry.getKey());
            if (advancement == null) {
                continue;
            }
            if (isExcluded(entry.getKey())) {
                continue;
            }
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            Collection<String> awarded = progress.getAwardedCriteria();
            for (String criterion : entry.getValue()) {
                if (!awarded.contains(criterion)) {
                    progress.awardCriteria(criterion);
                }
            }
        }
    }

    void applyGlobalAdvancementsToAllPlayers() {
        if (!enabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyGlobalAdvancementsToPlayer(player);
        }
    }

    void resetPlayersAdvancements() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (Advancement advancement : trackedAdvancements) {
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                Set<String> awarded = new HashSet<>(progress.getAwardedCriteria());
                for (String criterion : awarded) {
                    progress.revokeCriteria(criterion);
                }
            }
        }
    }

    private void pollOnce() {
        if (!enabled) {
            return;
        }
        if (Bukkit.getOnlinePlayers().isEmpty()) {
            return;
        }
        if (trackedAdvancements.isEmpty()) {
            return;
        }
        int processed = 0;
        while (processed < POLL_CHUNK_SIZE) {
            if (trackedAdvancements.isEmpty()) {
                return;
            }
            if (pollCursor >= trackedAdvancements.size()) {
                pollCursor = 0;
            }
            Advancement advancement = trackedAdvancements.get(pollCursor++);
            NamespacedKey key = advancement.getKey();
            if (isExcluded(key)) {
                processed++;
                continue;
            }
            Set<String> global = globalAdvancements.computeIfAbsent(key, unused -> new HashSet<>());
            boolean changed = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                for (String criterion : progress.getAwardedCriteria()) {
                    if (global.add(criterion)) {
                        changed = true;
                    }
                }
            }
            if (changed) {
                awardMissingCriteriaToAll(advancement, global);
            }
            processed++;
        }
    }

    private void awardMissingCriteriaToAll(Advancement advancement, Set<String> globalCriteria) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            AdvancementProgress progress = player.getAdvancementProgress(advancement);
            Collection<String> awarded = progress.getAwardedCriteria();
            for (String criterion : globalCriteria) {
                if (!awarded.contains(criterion)) {
                    progress.awardCriteria(criterion);
                }
            }
        }
    }

    private void rebuildAdvancementList() {
        trackedAdvancements.clear();
        Iterator<Advancement> iterator = plugin.getServer().advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            if (!isExcluded(advancement.getKey())) {
                trackedAdvancements.add(advancement);
            }
        }
        pollCursor = 0;
    }

    private boolean isExcluded(NamespacedKey key) {
        String keyString = key.toString();
        for (String prefix : excludedPrefixes) {
            if (keyString.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
