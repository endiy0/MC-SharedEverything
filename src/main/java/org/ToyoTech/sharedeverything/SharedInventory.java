package org.ToyoTech.sharedeverything;

import org.bukkit.inventory.ItemStack;

final class SharedInventory {
    private static final int STORAGE_SIZE = 36;
    private static final int ARMOR_SIZE = 4;
    private static final int OFFHAND_SIZE = 1;

    private final NmsBridge nms;
    private final Object items;
    private final Object armor;
    private final Object offhand;

    SharedInventory(NmsBridge nms) {
        this.nms = nms;
        this.items = nms.createEmptyList(STORAGE_SIZE);
        this.armor = nms.createEmptyList(ARMOR_SIZE);
        this.offhand = nms.createEmptyList(OFFHAND_SIZE);
    }

    void load(ItemStack[] storageContents, ItemStack[] armorContents, ItemStack offhandItem) {
        nms.fillList(items, storageContents == null ? new ItemStack[0] : storageContents, STORAGE_SIZE);
        nms.fillList(armor, armorContents == null ? new ItemStack[0] : armorContents, ARMOR_SIZE);
        ItemStack[] offhandArray = new ItemStack[]{offhandItem};
        nms.fillList(offhand, offhandArray, OFFHAND_SIZE);
    }

    void clear() {
        nms.fillList(items, new ItemStack[0], STORAGE_SIZE);
        nms.fillList(armor, new ItemStack[0], ARMOR_SIZE);
        nms.fillList(offhand, new ItemStack[0], OFFHAND_SIZE);
    }

    Object getItemsList() {
        return items;
    }

    Object getArmorList() {
        return armor;
    }

    Object getOffhandList() {
        return offhand;
    }

    ItemStack[] getStorageContents() {
        return nms.toBukkitArray(items, STORAGE_SIZE);
    }

    ItemStack[] getArmorContents() {
        return nms.toBukkitArray(armor, ARMOR_SIZE);
    }

    ItemStack getOffhandItem() {
        ItemStack[] offhandContents = nms.toBukkitArray(offhand, OFFHAND_SIZE);
        return offhandContents.length > 0 ? offhandContents[0] : null;
    }
}
