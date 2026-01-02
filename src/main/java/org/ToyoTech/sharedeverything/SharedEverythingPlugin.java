package org.ToyoTech.sharedeverything;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SharedEverythingPlugin extends JavaPlugin {
    private final Map<NamespacedKey, Set<String>> globalAdvancements = new HashMap<>();
    private final Map<UUID, Integer> syncDepth = new HashMap<>();
    private final Set<UUID> pendingInventorySnapshots = new HashSet<>();

    private GlobalInventory globalInventory;
    private DataStore dataStore;
    private AdvancementSyncManager advancementSyncManager;
    private BukkitTask autosaveTask;

    private boolean inventoryEnabled;
    private boolean includeEnderChest;
    private boolean keepInventoryOnDeath;
    private boolean advancementsEnabled;
    private int advancementsPollIntervalTicks;
    private List<String> advancementExclusions = Collections.emptyList();
    private int autosaveIntervalTicks;
    private long lastSaveEpochMillis;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        dataStore = new DataStore(this);
        DataStore.LoadedData loadedData = dataStore.load();
        globalInventory = loadedData.getGlobalInventory();
        globalAdvancements.clear();
        globalAdvancements.putAll(loadedData.getGlobalAdvancements());
        lastSaveEpochMillis = loadedData.getLastSaveEpochMillis();

        advancementSyncManager = new AdvancementSyncManager(this, globalAdvancements);
        advancementSyncManager.reload(advancementsEnabled, advancementsPollIntervalTicks, advancementExclusions);
        advancementSyncManager.start();

        getServer().getPluginManager().registerEvents(new InventorySyncListener(this), this);
        getServer().getPluginManager().registerEvents(new AdvancementSyncListener(this, advancementSyncManager), this);

        SharedEverythingCommand commandHandler = new SharedEverythingCommand(this);
        PluginCommand command = getCommand("sharedeverything");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        }

        scheduleAutosave();

        runNextTick(() -> {
            if (inventoryEnabled) {
                applyGlobalInventoryToAllPlayers();
            }
            if (advancementsEnabled) {
                advancementSyncManager.applyGlobalAdvancementsToAllPlayers();
            }
        });
    }

    @Override
    public void onDisable() {
        saveData(true);
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        if (advancementSyncManager != null) {
            advancementSyncManager.stop();
        }
    }

    void reloadPlugin() {
        reloadConfig();
        loadConfigValues();
        advancementSyncManager.reload(advancementsEnabled, advancementsPollIntervalTicks, advancementExclusions);
        advancementSyncManager.start();
        scheduleAutosave();

        if (inventoryEnabled) {
            applyGlobalInventoryToAllPlayers();
        }
        if (advancementsEnabled) {
            advancementSyncManager.applyGlobalAdvancementsToAllPlayers();
        }
    }

    boolean isInventorySyncEnabled() {
        return inventoryEnabled;
    }

    boolean isAdvancementSyncEnabled() {
        return advancementsEnabled;
    }

    boolean shouldKeepInventoryOnDeath() {
        return keepInventoryOnDeath;
    }

    boolean isSyncing(Player player) {
        return syncDepth.getOrDefault(player.getUniqueId(), 0) > 0;
    }

    void scheduleInventorySnapshot(Player player) {
        if (!inventoryEnabled) {
            return;
        }
        UUID playerId = player.getUniqueId();
        if (isSyncing(player)) {
            return;
        }
        if (!pendingInventorySnapshots.add(playerId)) {
            return;
        }
        getServer().getScheduler().runTaskLater(this, () -> {
            pendingInventorySnapshots.remove(playerId);
            if (!inventoryEnabled) {
                return;
            }
            if (!player.isOnline()) {
                return;
            }
            if (isSyncing(player)) {
                return;
            }
            captureAndBroadcastInventory(player);
        }, 1L);
    }

    void captureAndBroadcastInventory(Player player) {
        if (!inventoryEnabled) {
            return;
        }
        ItemStack[] fallbackEnder = globalInventory == null
                ? new ItemStack[27]
                : globalInventory.getEnderChestContents();
        globalInventory = GlobalInventory.fromPlayer(player, includeEnderChest, fallbackEnder);
        applyGlobalInventoryToAllPlayers();
    }

    void applyGlobalInventoryToPlayer(Player player) {
        if (!inventoryEnabled) {
            return;
        }
        if (globalInventory == null) {
            globalInventory = GlobalInventory.empty();
        }
        beginSync(player);
        try {
            globalInventory.applyToPlayer(player, includeEnderChest);
        } finally {
            endSyncLater(player);
        }
    }

    void applyGlobalInventoryToAllPlayers() {
        if (!inventoryEnabled) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyGlobalInventoryToPlayer(player);
        }
    }

    void resetGlobalInventory() {
        globalInventory = GlobalInventory.empty();
        if (inventoryEnabled) {
            applyGlobalInventoryToAllPlayers();
        }
    }

    void resetGlobalAdvancements() {
        globalAdvancements.clear();
        if (advancementSyncManager != null) {
            advancementSyncManager.resetPlayersAdvancements();
        }
    }

    void sendStatus(CommandSender sender) {
        sender.sendMessage("SharedEverything status:");
        sender.sendMessage("Inventory sync: " + inventoryEnabled
                + " (ender chest: " + includeEnderChest
                + ", keep inventory on death: " + keepInventoryOnDeath + ")");
        sender.sendMessage("Advancement sync: " + advancementsEnabled
                + " (poll interval ticks: " + advancementsPollIntervalTicks + ")");
        sender.sendMessage("Online players: " + Bukkit.getOnlinePlayers().size());
        sender.sendMessage("Last save: " + (lastSaveEpochMillis == 0L ? "never" : Instant.ofEpochMilli(lastSaveEpochMillis)));
    }

    void runNextTick(Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }

    private void beginSync(Player player) {
        UUID playerId = player.getUniqueId();
        syncDepth.merge(playerId, 1, Integer::sum);
    }

    private void endSyncLater(Player player) {
        UUID playerId = player.getUniqueId();
        getServer().getScheduler().runTaskLater(this, () -> {
            syncDepth.compute(playerId, (key, value) -> {
                if (value == null || value <= 1) {
                    return null;
                }
                return value - 1;
            });
        }, 1L);
    }

    private void scheduleAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        if (autosaveIntervalTicks <= 0) {
            return;
        }
        autosaveTask = getServer().getScheduler().runTaskTimer(this, () -> saveData(false), autosaveIntervalTicks, autosaveIntervalTicks);
    }

    private void saveData(boolean log) {
        if (dataStore == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (dataStore.save(globalInventory, globalAdvancements, now)) {
            lastSaveEpochMillis = now;
            if (log) {
                getLogger().info("Saved shared state to data.yml.");
            }
        }
    }

    private void loadConfigValues() {
        inventoryEnabled = getConfig().getBoolean("sync.inventory.enabled", true);
        includeEnderChest = getConfig().getBoolean("sync.inventory.include_ender_chest", true);
        keepInventoryOnDeath = getConfig().getBoolean("sync.inventory.keep_inventory_on_death", true);
        advancementsEnabled = getConfig().getBoolean("sync.advancements.enabled", true);
        advancementsPollIntervalTicks = getConfig().getInt("sync.advancements.poll_interval_ticks", 100);
        advancementExclusions = getConfig().getStringList("sync.advancements.exclude_namespaces_or_prefixes");
        autosaveIntervalTicks = getConfig().getInt("autosave.interval_ticks", 600);
    }
}

