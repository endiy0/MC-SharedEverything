package org.ToyoTech.sharedeverything;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

final class GlobalInventory {
    private final ItemStack[] storageContents;
    private final ItemStack[] armorContents;
    private final ItemStack offhandItem;
    private final ItemStack[] enderChestContents;

    GlobalInventory(ItemStack[] storageContents, ItemStack[] armorContents, ItemStack offhandItem, ItemStack[] enderChestContents) {
        this.storageContents = cloneContents(storageContents);
        this.armorContents = cloneContents(armorContents);
        this.offhandItem = cloneItem(offhandItem);
        this.enderChestContents = cloneContents(enderChestContents);
    }

    static GlobalInventory empty() {
        return new GlobalInventory(new ItemStack[36], new ItemStack[4], null, new ItemStack[27]);
    }

    static GlobalInventory fromPlayer(Player player, boolean includeEnderChest, ItemStack[] fallbackEnderChest) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] enderContents = includeEnderChest
                ? player.getEnderChest().getContents()
                : fallbackEnderChest;
        return new GlobalInventory(
                inventory.getStorageContents(),
                inventory.getArmorContents(),
                inventory.getItemInOffHand(),
                enderContents
        );
    }

    void applyToPlayer(Player player, boolean includeEnderChest) {
        PlayerInventory inventory = player.getInventory();
        inventory.setStorageContents(cloneContents(storageContents));
        inventory.setArmorContents(cloneContents(armorContents));
        inventory.setItemInOffHand(cloneItem(offhandItem));
        if (includeEnderChest) {
            player.getEnderChest().setContents(cloneContents(enderChestContents));
        }
        player.updateInventory();
    }

    ItemStack[] getStorageContents() {
        return cloneContents(storageContents);
    }

    ItemStack[] getArmorContents() {
        return cloneContents(armorContents);
    }

    ItemStack getOffhandItem() {
        return cloneItem(offhandItem);
    }

    ItemStack[] getEnderChestContents() {
        return cloneContents(enderChestContents);
    }

    private static ItemStack[] cloneContents(ItemStack[] contents) {
        if (contents == null) {
            return new ItemStack[0];
        }
        ItemStack[] clone = new ItemStack[contents.length];
        for (int i = 0; i < contents.length; i++) {
            clone[i] = cloneItem(contents[i]);
        }
        return clone;
    }

    private static ItemStack cloneItem(ItemStack item) {
        return item == null ? null : item.clone();
    }
}
