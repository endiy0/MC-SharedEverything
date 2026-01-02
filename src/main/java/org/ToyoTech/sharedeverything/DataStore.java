package org.ToyoTech.sharedeverything;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DataStore {
    private static final int STORAGE_SIZE = 36;
    private static final int ARMOR_SIZE = 4;
    private static final int ENDER_SIZE = 27;

    private final SharedEverythingPlugin plugin;
    private final File dataFile;

    DataStore(SharedEverythingPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "data.yml");
    }

    LoadedData load() {
        if (!dataFile.exists()) {
            return LoadedData.empty();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ItemStack[] storage = loadItemStackArray(config, "globalInventory.storage", STORAGE_SIZE);
        ItemStack[] armor = loadItemStackArray(config, "globalInventory.armor", ARMOR_SIZE);
        ItemStack[] offhandArray = loadItemStackArray(config, "globalInventory.offhand", 1);
        ItemStack offhand = offhandArray.length > 0 ? offhandArray[0] : null;
        ItemStack[] ender = loadItemStackArray(config, "globalInventory.enderChest", ENDER_SIZE);

        Map<NamespacedKey, Set<String>> advancements = new HashMap<>();
        ConfigurationSection advancementSection = config.getConfigurationSection("globalAdvancements");
        if (advancementSection != null) {
            for (String keyString : advancementSection.getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(keyString);
                if (key == null) {
                    continue;
                }
                List<String> criteriaList = advancementSection.getStringList(keyString);
                advancements.put(key, new HashSet<>(criteriaList));
            }
        }

        long lastSaveEpochMillis = config.getLong("lastSaveEpochMillis", 0L);
        GlobalInventory inventory = new GlobalInventory(storage, armor, offhand, ender);
        return new LoadedData(inventory, advancements, lastSaveEpochMillis);
    }

    boolean save(GlobalInventory inventory, Map<NamespacedKey, Set<String>> advancements, long lastSaveEpochMillis) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder.");
        }

        GlobalInventory safeInventory = inventory == null ? GlobalInventory.empty() : inventory;
        YamlConfiguration config = new YamlConfiguration();
        config.set("lastSaveEpochMillis", lastSaveEpochMillis);

        try {
            config.set("globalInventory.storage", ItemStackSerializer.itemStackArrayToBase64(safeInventory.getStorageContents()));
            config.set("globalInventory.armor", ItemStackSerializer.itemStackArrayToBase64(safeInventory.getArmorContents()));
            config.set("globalInventory.offhand", ItemStackSerializer.itemStackArrayToBase64(new ItemStack[]{safeInventory.getOffhandItem()}));
            config.set("globalInventory.enderChest", ItemStackSerializer.itemStackArrayToBase64(safeInventory.getEnderChestContents()));
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to serialize global inventory: " + e.getMessage());
            return false;
        }

        ConfigurationSection advancementSection = config.createSection("globalAdvancements");
        for (Map.Entry<NamespacedKey, Set<String>> entry : advancements.entrySet()) {
            List<String> criteria = new ArrayList<>(entry.getValue());
            advancementSection.set(entry.getKey().toString(), criteria);
        }

        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
            return false;
        }
        return true;
    }

    private ItemStack[] loadItemStackArray(YamlConfiguration config, String path, int defaultSize) {
        String base64 = config.getString(path);
        if (base64 == null || base64.isEmpty()) {
            return new ItemStack[defaultSize];
        }

        try {
            ItemStack[] decoded = ItemStackSerializer.itemStackArrayFromBase64(base64);
            return normalizeSize(decoded, defaultSize);
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().warning("Failed to decode inventory data at " + path + ": " + e.getMessage());
            return new ItemStack[defaultSize];
        }
    }

    private ItemStack[] normalizeSize(ItemStack[] input, int size) {
        ItemStack[] output = new ItemStack[size];
        if (input == null) {
            return output;
        }
        System.arraycopy(input, 0, output, 0, Math.min(input.length, size));
        return output;
    }

    static final class LoadedData {
        private final GlobalInventory globalInventory;
        private final Map<NamespacedKey, Set<String>> globalAdvancements;
        private final long lastSaveEpochMillis;

        LoadedData(GlobalInventory globalInventory, Map<NamespacedKey, Set<String>> globalAdvancements, long lastSaveEpochMillis) {
            this.globalInventory = globalInventory;
            this.globalAdvancements = globalAdvancements;
            this.lastSaveEpochMillis = lastSaveEpochMillis;
        }

        static LoadedData empty() {
            return new LoadedData(GlobalInventory.empty(), new HashMap<>(), 0L);
        }

        GlobalInventory getGlobalInventory() {
            return globalInventory;
        }

        Map<NamespacedKey, Set<String>> getGlobalAdvancements() {
            return globalAdvancements;
        }

        long getLastSaveEpochMillis() {
            return lastSaveEpochMillis;
        }
    }
}