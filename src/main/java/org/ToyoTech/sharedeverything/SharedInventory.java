package org.ToyoTech.sharedeverything;

import org.bukkit.inventory.ItemStack;

final class SharedInventory {
    private static final int STORAGE_SIZE = 36;

    private final NmsBridge nms;
    private final Object items;

    SharedInventory(NmsBridge nms) {
        this.nms = nms;
        this.items = nms.createEmptyList(STORAGE_SIZE);
    }

    void load(ItemStack[] storageContents) {
        nms.fillList(items, storageContents == null ? new ItemStack[0] : storageContents, STORAGE_SIZE);
    }

    void clear() {
        nms.fillList(items, new ItemStack[0], STORAGE_SIZE);
    }

    Object getItemsList() {
        return items;
    }

    ItemStack[] getStorageContents() {
        return nms.toBukkitArray(items, STORAGE_SIZE);
    }
}