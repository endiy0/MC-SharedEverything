package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private Field equipmentField;

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
            Object items = itemsField != null ? itemsField.get(inventory) : createEmptyList(36);
            
            Object armorSource = inventory;
            Object offhandSource = inventory;
            if (equipmentField != null) {
                Object equipment = equipmentField.get(inventory);
                if (equipment != null) {
                    armorSource = equipment;
                    offhandSource = equipment;
                }
            }

            Object armor = armorField != null ? armorField.get(armorSource) : createEmptyList(4);
            Object offhand = offhandField != null ? offhandField.get(offhandSource) : createEmptyList(1);
            return new InventoryLists(items, armor, offhand);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to read inventory lists", e);
        }
    }

    void setInventoryLists(Object inventory, Object items, Object armor, Object offhand) {
        resolveInventoryFields(inventory);
        try {
            if (itemsField != null) itemsField.set(inventory, items);
            
            Object armorTarget = inventory;
            Object offhandTarget = inventory;
            if (equipmentField != null) {
                Object equipment = equipmentField.get(inventory);
                if (equipment != null) {
                    armorTarget = equipment;
                    offhandTarget = equipment;
                }
            }

            if (armorField != null) armorField.set(armorTarget, armor);
            if (offhandField != null) offhandField.set(offhandTarget, offhand);
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
        Class<?> invClass = inventory.getClass();
        if (itemsField == null) {
            itemsField = findFieldByName(invClass, "items", List.class);
        }
        if (armorField == null) {
            armorField = findFieldByName(invClass, "armor", List.class);
        }
        if (offhandField == null) {
            offhandField = findFieldByName(invClass, "offhand", List.class);
        }
        if (compartmentsField == null) {
            compartmentsField = findFieldByName(invClass, "compartments", List.class);
        }
        
        // Try looking in 'equipment' field if armor/offhand are missing
        if (armorField == null || offhandField == null) {
             try {
                equipmentField = findFieldByName(invClass, "equipment", null);
                
                if (equipmentField != null) {
                    Object equipment = equipmentField.get(inventory);
                    if (equipment != null) {
                         Class<?> equipmentClass = equipment.getClass();
                         
                         List<ListField> eqCandidates = new ArrayList<>();
                         for (Field field : findFieldsAssignable(equipmentClass, List.class)) {
                            try {
                                Object value = field.get(equipment);
                                if (looksLikeItemList(value)) {
                                    int size = ((List<?>) value).size();
                                    eqCandidates.add(new ListField(field, size));
                                }
                            } catch (Exception ignored) {}
                         }
                         
                         for (ListField cand : eqCandidates) {
                             if (armorField == null && cand.size() == 4) {
                                 armorField = cand.field();
                             }
                             if (offhandField == null && cand.size() == 1) {
                                 offhandField = cand.field();
                             }
                         }
                    }
                }
             } catch (Exception e) {
                 Bukkit.getLogger().warning("Error inspecting equipment field: " + e.getMessage());
             }
        }

        if (itemsField == null || armorField == null || offhandField == null) {
            List<ListField> candidates = new ArrayList<>();
            List<String> candidateDebugInfo = new ArrayList<>();
            for (Field field : findFieldsAssignable(invClass, List.class)) {
                try {
                    Object value = field.get(inventory);
                    if (!looksLikeItemList(value)) {
                        candidateDebugInfo.add(field.getName() + " (size=" + (value instanceof List ? ((List<?>) value).size() : "null") + ", ignored)");
                        continue;
                    }
                    int size = ((List<?>) value).size();
                    candidates.add(new ListField(field, size));
                    candidateDebugInfo.add(field.getName() + " (size=" + size + ", accepted)");
                } catch (IllegalAccessException ignored) {
                }
            }
            if (candidates.isEmpty()) {
                Bukkit.getLogger().severe("No candidate inventory fields found in " + invClass.getName());
            }

            Set<Field> used = new HashSet<>();
            if (itemsField != null) {
                used.add(itemsField);
            }
            if (armorField != null) {
                used.add(armorField);
            }
            if (offhandField != null) {
                used.add(offhandField);
            }
            ListField max = null;
            ListField min = null;
            ListField size4 = null;
            ListField size1 = null;
            for (ListField candidate : candidates) {
                if (used.contains(candidate.field())) {
                    continue;
                }
                if (max == null || candidate.size() > max.size()) {
                    max = candidate;
                }
                if (min == null || candidate.size() < min.size()) {
                    min = candidate;
                }
                if (candidate.size() == 4 && size4 == null) {
                    size4 = candidate;
                }
                if (candidate.size() == 1 && size1 == null) {
                    size1 = candidate;
                }
            }
            if (itemsField == null && max != null) {
                itemsField = max.field();
                used.add(itemsField);
            }
            if (offhandField == null && size1 != null && !used.contains(size1.field())) {
                offhandField = size1.field();
                used.add(offhandField);
            }
            if (armorField == null && size4 != null && !used.contains(size4.field())) {
                armorField = size4.field();
                used.add(armorField);
            }
            if (offhandField == null && min != null && !used.contains(min.field())) {
                offhandField = min.field();
                used.add(offhandField);
            }
            if (armorField == null) {
                for (ListField candidate : candidates) {
                    if (!used.contains(candidate.field())) {
                        armorField = candidate.field();
                        used.add(armorField);
                        break;
                    }
                }
            }
            if (itemsField == null) {
                for (ListField candidate : candidates) {
                    if (!used.contains(candidate.field())) {
                        itemsField = candidate.field();
                        used.add(itemsField);
                        break;
                    }
                }
            }
            
            if (itemsField == null || armorField == null || offhandField == null) {
                if (itemsField == null) {
                    Bukkit.getLogger().warning("Could not find main inventory items field! This is a serious issue.");
                } else {
                    Bukkit.getLogger().info("Some inventory sub-lists (Armor/Offhand) not found in Inventory class. This is expected on Minecraft 1.21.1+; using Bukkit API fallback.");
                }
            }
        }
        
        if (compartmentsField == null && itemsField != null && armorField != null && offhandField != null) {
            for (Field field : findFieldsAssignable(invClass, List.class)) {
                try {
                    Object value = field.get(inventory);
                    if (value instanceof List<?> list && list.size() >= 3) {
                        Object items = itemsField.get(inventory);
                        Object armorSource = (equipmentField != null && equipmentField.get(inventory) != null) ? equipmentField.get(inventory) : inventory;
                        Object offhandSource = (equipmentField != null && equipmentField.get(inventory) != null) ? equipmentField.get(inventory) : inventory;
                        
                        Object armor = armorField.get(armorSource);
                        Object offhand = offhandField.get(offhandSource);
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
                if (fieldType == null || fieldType.isAssignableFrom(field.getType())) {
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

    private boolean looksLikeItemList(Object value) {
        if (!(value instanceof List<?> list) || list.isEmpty()) {
            return false;
        }
        Object sample = null;
        for (Object element : list) {
            if (element != null) {
                sample = element;
                break;
            }
        }
        if (sample == null) {
            return false;
        }
        return emptyNmsItem.getClass().isInstance(sample);
    }

    private record ListField(Field field, int size) {}

    record InventoryLists(Object items, Object armor, Object offhand) {}
}
