package org.ToyoTech.sharedeverything;

import org.bukkit.inventory.ItemStack;

final class SharedOffHand {
    private static final int SIZE = 1;

    private final NmsBridge nms;
    private final Object list;

    SharedOffHand(NmsBridge nms) {
        this.nms = nms;
        this.list = nms.createEmptyList(SIZE);
    }

    SharedOffHand(NmsBridge nms, Object existingStorage) {
        this.nms = nms;
        this.list = existingStorage != null ? existingStorage : nms.createEmptyList(SIZE);
    }

    void load(ItemStack item) {
        ItemStack[] array = new ItemStack[]{item};
        nms.fillList(list, array, SIZE);
    }

    void clear() {
        nms.fillList(list, new ItemStack[0], SIZE);
    }

    Object getList() {
        return list;
    }

    ItemStack getItem() {
        ItemStack[] contents = nms.toBukkitArray(list, SIZE);
        return contents.length > 0 ? contents[0] : null;
    }
}
