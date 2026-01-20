package org.ToyoTech.sharedeverything;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class SharedDataStore {
    private static final int STORAGE_SIZE = 36;
    private static final int ARMOR_SIZE = 4;
    private static final int OFFHAND_SIZE = 1;

    private final SharedEverythingPlugin plugin;
    private final File inventoryFile;
    private final File advancementFile;
    private final File teamsDir;

    SharedDataStore(SharedEverythingPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        inventoryFile = new File(dataFolder, "inventory.yml");
        advancementFile = new File(dataFolder, "advancements.yml");
        teamsDir = new File(dataFolder, "teams");
    }

    void load(SharedInventory sharedInventory, SharedArmor sharedArmor, SharedOffHand sharedOffHand,
              Map<String, SharedInventory> teamInventories, Map<String, SharedArmor> teamArmors, Map<String, SharedOffHand> teamOffHands,
              Set<NamespacedKey> advancements) {
        if (!inventoryFile.exists()) {
            sharedInventory.clear();
            sharedArmor.clear();
            sharedOffHand.clear();
        } else {
            FileConfiguration inventoryConfig = YamlConfiguration.loadConfiguration(inventoryFile);
            ItemStack[] items = deserializeItems(inventoryConfig.getMapList("items"), STORAGE_SIZE);
            ItemStack[] armor = deserializeItems(inventoryConfig.getMapList("armor"), ARMOR_SIZE);
            ItemStack[] offhand = deserializeItems(inventoryConfig.getMapList("offhand"), OFFHAND_SIZE);
            sharedInventory.load(items);
            sharedArmor.load(armor);
            sharedOffHand.load(offhand.length > 0 ? offhand[0] : null);
        }

        if (advancementFile.exists()) {
            FileConfiguration advancementConfig = YamlConfiguration.loadConfiguration(advancementFile);
            List<String> advancementList = advancementConfig.getStringList("advancements");
            for (String keyString : advancementList) {
                NamespacedKey key = NamespacedKey.fromString(keyString);
                if (key != null) {
                    advancements.add(key);
                }
            }
        }

        if (teamsDir.exists() && teamsDir.isDirectory()) {
            File[] files = teamsDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (files != null) {
                for (File file : files) {
                    String teamName = file.getName().substring(0, file.getName().length() - 4);
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    ItemStack[] items = deserializeItems(config.getMapList("items"), STORAGE_SIZE);
                    ItemStack[] armor = deserializeItems(config.getMapList("armor"), ARMOR_SIZE);
                    ItemStack[] offhand = deserializeItems(config.getMapList("offhand"), OFFHAND_SIZE);
                    
                    SharedInventory teamInventory = new SharedInventory(plugin.getNmsBridge());
                    teamInventory.load(items);
                    teamInventories.put(teamName, teamInventory);

                    SharedArmor teamArmor;
                    SharedOffHand teamOffHand;

                    if (plugin.getNmsBridge().isUsingEquipmentMap()) {
                        Object sharedMap = plugin.getNmsBridge().createEquipmentMap();
                        teamArmor = new SharedArmor(plugin.getNmsBridge(), sharedMap);
                        teamOffHand = new SharedOffHand(plugin.getNmsBridge(), sharedMap);
                    } else {
                        teamArmor = new SharedArmor(plugin.getNmsBridge());
                        teamOffHand = new SharedOffHand(plugin.getNmsBridge());
                    }

                    teamArmor.load(armor);
                    teamInventories.put(teamName, teamInventory);
                    teamArmors.put(teamName, teamArmor);

                    teamOffHand.load(offhand.length > 0 ? offhand[0] : null);
                    teamOffHands.put(teamName, teamOffHand);
                }
            }
        }
    }

    void save(SharedInventory sharedInventory, SharedArmor sharedArmor, SharedOffHand sharedOffHand,
              Map<String, SharedInventory> teamInventories, Map<String, SharedArmor> teamArmors, Map<String, SharedOffHand> teamOffHands,
              Set<NamespacedKey> advancements) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder.");
        }

        FileConfiguration inventoryConfig = new YamlConfiguration();
        inventoryConfig.set("items", serializeItems(sharedInventory.getStorageContents()));
        inventoryConfig.set("armor", serializeItems(sharedArmor.getContents()));
        inventoryConfig.set("offhand", serializeItems(new ItemStack[]{sharedOffHand.getItem()}));
        saveConfig(inventoryConfig, inventoryFile, "inventory.yml");

        FileConfiguration advancementConfig = new YamlConfiguration();
        List<String> advancementList = new ArrayList<>();
        for (NamespacedKey key : advancements) {
            advancementList.add(key.toString());
        }
        advancementConfig.set("advancements", advancementList);
        saveConfig(advancementConfig, advancementFile, "advancements.yml");

        if (!teamsDir.exists() && !teamsDir.mkdirs()) {
            plugin.getLogger().warning("Unable to create teams folder.");
        }
        
        // Collect all team names
        Set<String> teamNames = teamInventories.keySet();
        // (Assuming all maps have same keys or we handle union, but typically they are created together)
        
        for (String teamName : teamNames) {
            FileConfiguration teamConfig = new YamlConfiguration();
            
            SharedInventory teamInv = teamInventories.get(teamName);
            if (teamInv != null) {
                teamConfig.set("items", serializeItems(teamInv.getStorageContents()));
            }

            SharedArmor teamArmor = teamArmors.get(teamName);
            if (teamArmor != null) {
                teamConfig.set("armor", serializeItems(teamArmor.getContents()));
            }

            SharedOffHand teamOffHand = teamOffHands.get(teamName);
            if (teamOffHand != null) {
                teamConfig.set("offhand", serializeItems(new ItemStack[]{teamOffHand.getItem()}));
            }
            
            File teamFile = new File(teamsDir, teamName + ".yml");
            saveConfig(teamConfig, teamFile, "team inventory");
        }
    }

    private List<Map<String, Object>> serializeItems(ItemStack[] items) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        if (items == null) {
            return serialized;
        }
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR) {
                serialized.add(new HashMap<>());
            } else {
                serialized.add(item.serialize());
            }
        }
        return serialized;
    }

    private ItemStack[] deserializeItems(List<Map<?, ?>> maps, int size) {
        ItemStack[] items = new ItemStack[size];
        if (maps == null) {
            return items;
        }
        int limit = Math.min(maps.size(), size);
        for (int i = 0; i < limit; i++) {
            Map<?, ?> data = maps.get(i);
            if (data == null || data.isEmpty()) {
                items[i] = null;
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> casted = (Map<String, Object>) data;
            try {
                items[i] = ItemStack.deserialize(casted);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to deserialize item at index " + i + ": " + e.getMessage());
                items[i] = null;
            }
        }
        return items;
    }

    private void saveConfig(FileConfiguration config, File file, String label) {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save " + label + ": " + e.getMessage());
        }
    }
}