package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

final class NmsBridge {
    private final Method craftPlayerGetHandle;
    private final Method asBukkitCopy;
    private final Method asNmsCopy;
    private final Method nonNullListWithSize;
    private final Object emptyNmsItem;
    private final Class<?> nonNullListClass;
    private final Class<?> inventoryClass;

    private Field handleInventoryField;
    private Field itemsField;
    private Field armorField;
    private Field offhandField;
    private Field compartmentsField;

    NmsBridge() {
        try {
            String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
            Class<?> craftPlayerClass = Class.forName(craftPackage + ".entity.CraftPlayer");
            craftPlayerGetHandle = craftPlayerClass.getMethod("getHandle");

            Class<?> craftItemStackClass = Class.forName(craftPackage + ".inventory.CraftItemStack");
            Class<?> nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            asBukkitCopy = craftItemStackClass.getMethod("asBukkitCopy", nmsItemStackClass);
            asNmsCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);

            inventoryClass = Class.forName("net.minecraft.world.entity.player.Inventory");
            nonNullListClass = Class.forName("net.minecraft.core.NonNullList");
            emptyNmsItem = nmsItemStackClass.getField("EMPTY").get(null);
            nonNullListWithSize = nonNullListClass.getMethod("withSize", int.class, Object.class);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize NMS bridge", e);
        }
    }

    Object getPlayerInventory(Player player) {
        try {
            Object handle = craftPlayerGetHandle.invoke(player);
            if (handleInventoryField == null) {
                handleInventoryField = findInventoryField(handle.getClass());
            }
            return handleInventoryField.get(handle);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to access player inventory handle", e);
        }
    }

    InventoryLists getInventoryLists(Object inventory) {
        resolveInventoryFields(inventory);
        try {
            Object items = itemsField.get(inventory);
            Object armor = armorField.get(inventory);
            Object offhand = offhandField.get(inventory);
            return new InventoryLists(items, armor, offhand);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to read inventory lists", e);
        }
    }

    void setInventoryLists(Object inventory, Object items, Object armor, Object offhand) {
        resolveInventoryFields(inventory);
        try {
            itemsField.set(inventory, items);
            armorField.set(inventory, armor);
            offhandField.set(inventory, offhand);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set inventory lists", e);
        }
        if (compartmentsField != null) {
            try {
                compartmentsField.set(inventory, List.of(items, armor, offhand));
            } catch (IllegalAccessException e) {
                Bukkit.getLogger().warning("Failed to update inventory compartments list: " + e.getMessage());
            }
        }
    }

    Object createEmptyList(int size) {
        try {
            return nonNullListWithSize.invoke(null, size, emptyNmsItem);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to create NMS item list", e);
        }
    }

    Object toNmsItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return emptyNmsItem;
        }
        try {
            return asNmsCopy.invoke(null, item);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to convert item to NMS", e);
        }
    }

    ItemStack toBukkitItem(Object nmsItem) {
        if (nmsItem == null || nmsItem == emptyNmsItem) {
            return null;
        }
        try {
            ItemStack item = (ItemStack) asBukkitCopy.invoke(null, nmsItem);
            if (item == null || item.getType() == Material.AIR) {
                return null;
            }
            return item;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to convert item to Bukkit", e);
        }
    }

    ItemStack[] toBukkitArray(Object nmsList, int size) {
        ItemStack[] result = new ItemStack[size];
        List<?> list = castList(nmsList);
        for (int i = 0; i < size && i < list.size(); i++) {
            result[i] = toBukkitItem(list.get(i));
        }
        return result;
    }

    void fillList(Object nmsList, ItemStack[] items, int size) {
        List<Object> list = castList(nmsList);
        for (int i = 0; i < size && i < list.size(); i++) {
            ItemStack item = i < items.length ? items[i] : null;
            list.set(i, toNmsItem(item));
        }
    }

    private void resolveInventoryFields(Object inventory) {
        if (itemsField != null && armorField != null && offhandField != null && compartmentsField != null) {
            return;
        }
        Class<?> invClass = inventory.getClass();
        itemsField = findFieldByName(invClass, "items", nonNullListClass);
        armorField = findFieldByName(invClass, "armor", nonNullListClass);
        offhandField = findFieldByName(invClass, "offhand", nonNullListClass);
        compartmentsField = findFieldByName(invClass, "compartments", List.class);

        if (itemsField == null || armorField == null || offhandField == null) {
            List<Field> listFields = findFieldsAssignable(invClass, nonNullListClass);
            if (listFields.isEmpty()) {
                throw new IllegalStateException("Failed to locate inventory item lists");
            }
            for (Field field : listFields) {
                try {
                    Object value = field.get(inventory);
                    int size = castList(value).size();
                    if (size == 36 && itemsField == null) {
                        itemsField = field;
                    } else if (size == 4 && armorField == null) {
                        armorField = field;
                    } else if (size == 1 && offhandField == null) {
                        offhandField = field;
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
            if (itemsField == null || armorField == null || offhandField == null) {
                throw new IllegalStateException("Failed to resolve inventory list fields");
            }
        }
        if (compartmentsField == null) {
            for (Field field : findFieldsAssignable(invClass, List.class)) {
                try {
                    Object value = field.get(inventory);
                    if (value instanceof List<?> list && list.size() >= 3) {
                        Object items = itemsField.get(inventory);
                        Object armor = armorField.get(inventory);
                        Object offhand = offhandField.get(inventory);
                        if (list.contains(items) && list.contains(armor) && list.contains(offhand)) {
                            compartmentsField = field;
                            break;
                        }
                    }
                } catch (IllegalAccessException ignored) {
                }
            }
        }
    }

    private Field findInventoryField(Class<?> handleClass) {
        for (Field field : findFieldsAssignable(handleClass, inventoryClass)) {
            return field;
        }
        throw new IllegalStateException("Failed to locate player inventory field");
    }

    private Field findFieldByName(Class<?> type, String name, Class<?> fieldType) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(name);
                if (fieldType.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private List<Field> findFieldsAssignable(Class<?> type, Class<?> fieldType) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (fieldType.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    private List<Object> castList(Object list) {
        return (List<Object>) list;
    }

    record InventoryLists(Object items, Object armor, Object offhand) {}
}
