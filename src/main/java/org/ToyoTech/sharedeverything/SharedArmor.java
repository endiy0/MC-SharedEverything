package org.ToyoTech.sharedeverything;

import org.bukkit.inventory.ItemStack;

final class SharedArmor {
    private static final int SIZE = 4;

    private final NmsBridge nms;
    private final Object list;

    SharedArmor(NmsBridge nms) {
        this.nms = nms;
        this.list = nms.createEmptyList(SIZE);
    }

    SharedArmor(NmsBridge nms, Object existingStorage) {
        this.nms = nms;
        this.list = existingStorage != null ? existingStorage : nms.createEmptyList(SIZE);
    }

    void load(ItemStack[] contents) {
        nms.fillList(list, contents == null ? new ItemStack[0] : contents, SIZE);
    }

    void clear() {
        nms.fillList(list, new ItemStack[0], SIZE);
    }

    Object getList() {
        return list;
    }

    ItemStack[] getContents() {
        return nms.toBukkitArray(list, SIZE);
    }
}
