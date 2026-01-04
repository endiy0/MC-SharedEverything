package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class AdvancementShareManager {
    private final Set<NamespacedKey> sharedAdvancements = new HashSet<>();

    Set<NamespacedKey> getSharedAdvancements() {
        return sharedAdvancements;
    }

    void recordAdvancement(Player player, Advancement advancement) {
        if (advancement == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (!progress.isDone()) {
            return;
        }
        if (sharedAdvancements.add(advancement.getKey())) {
            applyAdvancementToAll(advancement);
        }
    }

    void applyToPlayer(Player player) {
        for (NamespacedKey key : sharedAdvancements) {
            Advancement advancement = Bukkit.getAdvancement(key);
            if (advancement == null) {
                continue;
            }
            awardAllCriteria(player, advancement);
        }
    }

    void applyToAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyToPlayer(player);
        }
    }

    void resetAdvancements() {
        sharedAdvancements.clear();
        Iterator<Advancement> iterator = Bukkit.advancementIterator();
        while (iterator.hasNext()) {
            Advancement advancement = iterator.next();
            for (Player player : Bukkit.getOnlinePlayers()) {
                AdvancementProgress progress = player.getAdvancementProgress(advancement);
                for (String criterion : progress.getAwardedCriteria()) {
                    progress.revokeCriteria(criterion);
                }
            }
        }
    }

    private void applyAdvancementToAll(Advancement advancement) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            awardAllCriteria(player, advancement);
        }
    }

    private void awardAllCriteria(Player player, Advancement advancement) {
        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        if (progress.isDone()) {
            return;
        }
        for (String criterion : progress.getRemainingCriteria()) {
            progress.awardCriteria(criterion);
        }
    }
}
