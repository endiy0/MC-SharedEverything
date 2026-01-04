package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public final class SharedEverythingPlugin extends JavaPlugin {
    private static final int TEAM_WATCH_INTERVAL_TICKS = 20;

    private final Map<String, SharedInventory> teamInventories = new HashMap<>();

    private NmsBridge nmsBridge;
    private SharedInventory sharedInventory;
    private SharedInventoryManager inventoryManager;
    private SharedDataStore dataStore;
    private AdvancementShareManager advancementShareManager;
    private BukkitTask autosaveTask;
    private BukkitTask teamWatchTask;

    private boolean inventoryEnabled;
    private boolean advancementEnabled;
    private boolean announceDeath;
    private boolean teamInventoryEnabled;
    private boolean keepInventoryOnDeath;
    private int autosaveIntervalTicks;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfigValues();

        try {
            nmsBridge = new NmsBridge();
        } catch (IllegalStateException e) {
            getLogger().severe("SharedEverything failed to initialize: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        sharedInventory = new SharedInventory(nmsBridge);
        inventoryManager = new SharedInventoryManager(nmsBridge, sharedInventory, teamInventories);
        advancementShareManager = new AdvancementShareManager();
        dataStore = new SharedDataStore(this);
        dataStore.load(sharedInventory, teamInventories, advancementShareManager.getSharedAdvancements());

        getServer().getPluginManager().registerEvents(new SharedEverythingListener(this), this);

        SharedEverythingCommand commandHandler = new SharedEverythingCommand(this);
        PluginCommand command = getCommand("sharedeverything");
        if (command != null) {
            command.setExecutor(commandHandler);
            command.setTabCompleter(commandHandler);
        }

        scheduleAutosave();
        startTeamWatcher();

        for (var player : Bukkit.getOnlinePlayers()) {
            inventoryManager.updateInventory(player, inventoryEnabled, teamInventoryEnabled);
            if (advancementEnabled) {
                advancementShareManager.applyToPlayer(player);
            }
        }
    }

    @Override
    public void onDisable() {
        saveSharedData(true);
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        if (teamWatchTask != null) {
            teamWatchTask.cancel();
            teamWatchTask = null;
        }
        if (inventoryManager != null) {
            for (var player : Bukkit.getOnlinePlayers()) {
                inventoryManager.applyPersonalInventory(player);
                inventoryManager.clearPlayerState(player);
            }
        }
    }

    void reloadPlugin(CommandSender sender) {
        reloadConfig();
        loadConfigValues();
        scheduleAutosave();
        startTeamWatcher();
        inventoryManager.applyToAllPlayers(inventoryEnabled, teamInventoryEnabled);
        if (advancementEnabled) {
            advancementShareManager.applyToAllPlayers();
        }
        sender.sendMessage("SharedEverything reloaded.");
    }

    void setInventoryEnabled(boolean enabled) {
        inventoryEnabled = enabled;
        getConfig().set("inventory", enabled);
        saveConfig();
        inventoryManager.applyToAllPlayers(inventoryEnabled, teamInventoryEnabled);
    }

    void setAdvancementEnabled(boolean enabled) {
        advancementEnabled = enabled;
        getConfig().set("advancement", enabled);
        saveConfig();
        if (advancementEnabled) {
            advancementShareManager.applyToAllPlayers();
        }
    }

    void setAnnounceDeathEnabled(boolean enabled) {
        announceDeath = enabled;
        getConfig().set("announcedeath", enabled);
        saveConfig();
    }

    void setTeamInventoryEnabled(boolean enabled) {
        teamInventoryEnabled = enabled;
        getConfig().set("teaminventory", enabled);
        saveConfig();
        inventoryManager.applyToAllPlayers(inventoryEnabled, teamInventoryEnabled);
    }

    void setKeepInventoryOnDeath(boolean enabled) {
        keepInventoryOnDeath = enabled;
        getConfig().set("keep_inventory_on_death", enabled);
        saveConfig();
    }

    void resetSharedInventory() {
        sharedInventory.clear();
        for (SharedInventory inventory : teamInventories.values()) {
            inventory.clear();
        }
        inventoryManager.applyToAllPlayers(inventoryEnabled, teamInventoryEnabled);
    }

    void resetAdvancements() {
        advancementShareManager.resetAdvancements();
    }

    void sendStatus(CommandSender sender) {
        sender.sendMessage("SharedEverything status:");
        sender.sendMessage("inventory: " + inventoryEnabled);
        sender.sendMessage("advancement: " + advancementEnabled);
        sender.sendMessage("announcedeath: " + announceDeath);
        sender.sendMessage("teaminventory: " + teamInventoryEnabled);
        sender.sendMessage("keep_inventory_on_death: " + keepInventoryOnDeath);
    }

    void saveSharedData(boolean log) {
        if (dataStore == null) {
            return;
        }
        dataStore.save(sharedInventory, teamInventories, advancementShareManager.getSharedAdvancements());
        if (log) {
            getLogger().info("Saved shared data.");
        }
    }

    NmsBridge getNmsBridge() {
        return nmsBridge;
    }

    SharedInventoryManager getInventoryManager() {
        return inventoryManager;
    }

    AdvancementShareManager getAdvancementShareManager() {
        return advancementShareManager;
    }

    boolean isInventoryEnabled() {
        return inventoryEnabled;
    }

    boolean isAdvancementEnabled() {
        return advancementEnabled;
    }

    boolean isAnnounceDeathEnabled() {
        return announceDeath;
    }

    boolean isTeamInventoryEnabled() {
        return teamInventoryEnabled;
    }

    boolean shouldKeepInventoryOnDeath() {
        return keepInventoryOnDeath;
    }

    private void scheduleAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        if (autosaveIntervalTicks <= 0) {
            return;
        }
        autosaveTask = getServer().getScheduler().runTaskTimer(this, () -> saveSharedData(false), autosaveIntervalTicks, autosaveIntervalTicks);
    }

    private void startTeamWatcher() {
        if (teamWatchTask != null) {
            teamWatchTask.cancel();
            teamWatchTask = null;
        }
        teamWatchTask = getServer().getScheduler().runTaskTimer(this, new TeamInventoryWatcher(this), TEAM_WATCH_INTERVAL_TICKS, TEAM_WATCH_INTERVAL_TICKS);
    }

    private void loadConfigValues() {
        inventoryEnabled = getConfig().getBoolean("inventory", true);
        advancementEnabled = getConfig().getBoolean("advancement", true);
        announceDeath = getConfig().getBoolean("announcedeath", false);
        teamInventoryEnabled = getConfig().getBoolean("teaminventory", false);
        keepInventoryOnDeath = getConfig().getBoolean("keep_inventory_on_death", true);
        autosaveIntervalTicks = getConfig().getInt("autosave.interval_ticks", 600);
    }
}
