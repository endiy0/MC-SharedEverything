package org.ToyoTech.sharedeverything;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class NmsBridge {
    private final Method craftPlayerGetHandle;
    private final Method asBukkitCopy;
    private final Method asNmsCopy;
    private final Method nonNullListWithSize;
    private final Object emptyNmsItem;
    private final Class<?> nonNullListClass;
    private Class<?> inventoryClass;
    
    // 1.21+ Equipment Support
    private Class<?> equipmentSlotClass;
    private Object[] armorSlots; // FEET, LEGS, CHEST, HEAD
    private Object offhandSlot;  // OFFHAND

    private Field handleInventoryField;
    private Field itemsField;
    private Field armorField;
    private Field offhandField;
    private Field compartmentsField;
    private Field equipmentField;
    private Field equipmentItemsField; // The EnumMap field inside PlayerEquipment

    NmsBridge() {
        try {
            String craftPackage = Bukkit.getServer().getClass().getPackage().getName();
            Class<?> craftPlayerClass = Class.forName(craftPackage + ".entity.CraftPlayer");
            craftPlayerGetHandle = craftPlayerClass.getMethod("getHandle");

            Class<?> craftItemStackClass = Class.forName(craftPackage + ".inventory.CraftItemStack");
            Class<?> nmsItemStackClass = Class.forName("net.minecraft.world.item.ItemStack");
            asBukkitCopy = craftItemStackClass.getMethod("asBukkitCopy", nmsItemStackClass);
            asNmsCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);

            try {
                inventoryClass = Class.forName("net.minecraft.world.entity.player.Inventory");
            } catch (ClassNotFoundException e) {
                inventoryClass = Class.forName("net.minecraft.world.entity.player.PlayerInventory");
            }
            
            nonNullListClass = Class.forName("net.minecraft.core.NonNullList");
            emptyNmsItem = nmsItemStackClass.getField("EMPTY").get(null);
            nonNullListWithSize = nonNullListClass.getMethod("withSize", int.class, Object.class);

            // Initialize EquipmentSlot enums if available
            try {
                equipmentSlotClass = Class.forName("net.minecraft.world.entity.EquipmentSlot");
                armorSlots = new Object[4];
                // Note: Order in array is Boots, Leggings, Chest, Helmet
                // EquipmentSlot: FEET, LEGS, CHEST, HEAD
                armorSlots[0] = Enum.valueOf((Class<Enum>) equipmentSlotClass, "FEET");
                armorSlots[1] = Enum.valueOf((Class<Enum>) equipmentSlotClass, "LEGS");
                armorSlots[2] = Enum.valueOf((Class<Enum>) equipmentSlotClass, "CHEST");
                armorSlots[3] = Enum.valueOf((Class<Enum>) equipmentSlotClass, "HEAD");
                offhandSlot = Enum.valueOf((Class<Enum>) equipmentSlotClass, "OFFHAND");

                Bukkit.getLogger().info("NmsBridge: Resolved EquipmentSlots: " + 
                    java.util.Arrays.toString(armorSlots) + ", Offhand: " + offhandSlot);
                
                // Proactively resolve PlayerEquipment field in Inventory and its EnumMap for 1.21+
                try {
                     // 1. Find 'equipment' field in Inventory class
                     try {
                         equipmentField = inventoryClass.getDeclaredField("equipment");
                     } catch (NoSuchFieldException e) {
                         // Fallback loop
                         for (Field f : inventoryClass.getDeclaredFields()) {
                             if (f.getName().equals("equipment")) {
                                 equipmentField = f;
                                 break;
                             }
                         }
                     }
                     
                     if (equipmentField != null) {
                         equipmentField.setAccessible(true);
                         Class<?> fieldType = equipmentField.getType();
                         Bukkit.getLogger().info("NmsBridge: Found 'equipment' field of type: " + fieldType.getName());
                         
                         // 2. Find EnumMap field in that type
                         for (Field f : fieldType.getDeclaredFields()) {
                             f.setAccessible(true);
                             if (EnumMap.class.isAssignableFrom(f.getType())) {
                                 equipmentItemsField = f;
                                 Bukkit.getLogger().info("NmsBridge: Found EnumMap field: " + f.getName());
                                 break;
                             }
                         }
                     } else {
                         Bukkit.getLogger().warning("NmsBridge: Could not find 'equipment' field in Inventory class: " + inventoryClass.getName());
                     }

                     Bukkit.getLogger().info("NmsBridge: Proactive 1.21 Check - EquipmentField: " + (equipmentField != null) + 
                                            ", ItemsField: " + (equipmentItemsField != null));
                } catch (Exception e) {
                    Bukkit.getLogger().warning("NmsBridge: Proactive setup failed: " + e.getMessage());
                }
            } catch (Exception e) {
                // Ignore if not on 1.21+ or structure is different
            }

        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to initialize NMS bridge", e);
        }
    }

    boolean isUsingEquipmentMap() {
        return equipmentItemsField != null;
    }

    Object createEquipmentMap() {
        if (!isUsingEquipmentMap()) return null;
        return new EnumMap((Class<Enum>) equipmentSlotClass);
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

    InventoryLists getInventoryLists(Object inventory, Player player) {
        resolveInventoryFields(inventory);
        try {
            Object items = itemsField != null ? itemsField.get(inventory) : createEmptyList(36);
            
            Object armor = null;
            Object offhand = null;

            if (isUsingEquipmentMap()) {
                Object equipment = equipmentField.get(inventory);
                if (equipment != null) {
                    Object map = equipmentItemsField.get(equipment);
                    armor = map;
                    offhand = map;
                }
            } else {
                Object equipment = null;
                if (equipmentField != null) {
                    equipment = equipmentField.get(inventory);
                }

                armor = createEmptyList(4);
                if (armorField != null) {
                    if (armorField.getDeclaringClass().isInstance(inventory)) {
                        armor = armorField.get(inventory);
                    } else if (equipment != null && armorField.getDeclaringClass().isInstance(equipment)) {
                        armor = armorField.get(equipment);
                    }
                }

                offhand = createEmptyList(1);
                if (offhandField != null) {
                    if (offhandField.getDeclaringClass().isInstance(inventory)) {
                        offhand = offhandField.get(inventory);
                    } else if (equipment != null && offhandField.getDeclaringClass().isInstance(equipment)) {
                        offhand = offhandField.get(equipment);
                    }
                }
            }

            if (player != null) {
                if (armor == null || (armor instanceof List && ((List)armor).isEmpty()) || (armor instanceof Map && ((Map)armor).isEmpty())) {
                    if (armor == null) {
                        armor = createEmptyList(4); // Fallback to list if totally lost
                        ItemStack[] armorContents = player.getInventory().getArmorContents();
                        fillList(armor, armorContents, 4);
                    }
                }
                if (offhand == null) {
                    offhand = createEmptyList(1);
                    ItemStack offhandItem = player.getInventory().getItemInOffHand();
                    fillList(offhand, new ItemStack[]{offhandItem}, 1);
                }
            }

            return new InventoryLists(items, armor, offhand);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to read inventory lists", e);
        }
    }

    void setInventoryLists(Object inventory, Player player, Object items, Object armor, Object offhand) {
        resolveInventoryFields(inventory);
        try {
            if (itemsField != null) itemsField.set(inventory, items);
            
            if (isUsingEquipmentMap()) {
                Object equipment = equipmentField.get(inventory);
                if (equipment != null) {
                    // armor and offhand should be the SAME map object in this mode
                    Bukkit.getLogger().info("NmsBridge: Linking shared EnumMap to PlayerEquipment. Map hash: " + System.identityHashCode(armor));
                    equipmentItemsField.set(equipment, armor); 
                } else {
                    Bukkit.getLogger().warning("NmsBridge: Equipment object is null during set!");
                }
            } else {
                Object equipment = null;
                if (equipmentField != null) {
                    equipment = equipmentField.get(inventory);
                }

                if (armorField != null) {
                    if (armorField.getDeclaringClass().isInstance(inventory)) {
                        armorField.set(inventory, armor);
                    } else if (equipment != null && armorField.getDeclaringClass().isInstance(equipment)) {
                        armorField.set(equipment, armor);
                    }
                } else if (player != null) {
                    player.getInventory().setArmorContents(toBukkitArray(armor, 4));
                }

                if (offhandField != null) {
                    if (offhandField.getDeclaringClass().isInstance(inventory)) {
                        offhandField.set(inventory, offhand);
                    } else if (equipment != null && offhandField.getDeclaringClass().isInstance(equipment)) {
                        offhandField.set(equipment, offhand);
                    }
                } else if (player != null) {
                    ItemStack[] offhandItems = toBukkitArray(offhand, 1);
                    player.getInventory().setItemInOffHand(offhandItems.length > 0 ? offhandItems[0] : null);
                }
            }

        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Failed to set inventory lists", e);
        }
        if (compartmentsField != null) {
            try {
                compartmentsField.set(inventory, List.of(items, armor, offhand));
            } catch (Exception e) {
                // Ignore
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

    ItemStack[] toBukkitArray(Object nmsListOrMap, int size) {
        ItemStack[] result = new ItemStack[size];
        
        if (nmsListOrMap instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) nmsListOrMap;
            // DEBUG: Print map content to see what's inside
            Bukkit.getLogger().info("NmsBridge: Serializing Map (size=" + size + "). Content: " + map);

            if (size == 4) { // Armor
                for (int i = 0; i < 4; i++) {
                    result[i] = toBukkitItem(map.get(armorSlots[i]));
                }
            } else if (size == 1) { // Offhand
                result[0] = toBukkitItem(map.get(offhandSlot));
            }
            return result;
        }

        List<?> list = castList(nmsListOrMap);
        for (int i = 0; i < size && i < list.size(); i++) {
            result[i] = toBukkitItem(list.get(i));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    void fillList(Object nmsListOrMap, ItemStack[] items, int size) {
        if (nmsListOrMap instanceof Map) {
            Map<Object, Object> map = (Map<Object, Object>) nmsListOrMap;
            if (size == 4) { // Armor
                for (int i = 0; i < 4; i++) {
                    ItemStack item = i < items.length ? items[i] : null;
                    map.put(armorSlots[i], toNmsItem(item));
                }
            } else if (size == 1) { // Offhand
                ItemStack item = items.length > 0 ? items[0] : null;
                map.put(offhandSlot, toNmsItem(item));
            }
            return;
        }

        List<Object> list = castList(nmsListOrMap);
        for (int i = 0; i < size && i < list.size(); i++) {
            ItemStack item = i < items.length ? items[i] : null;
            if (i < list.size()) {
                list.set(i, toNmsItem(item));
            }
        }
    }

    private void resolveInventoryFields(Object inventory) {
        if (itemsField != null && (armorField != null || equipmentItemsField != null)) return;

        Class<?> invClass = inventory.getClass();
        
        if (itemsField == null) itemsField = findFieldByName(invClass, "items", List.class);
        if (armorField == null) armorField = findFieldByName(invClass, "armor", List.class);
        if (offhandField == null) offhandField = findFieldByName(invClass, "offhand", List.class);
        if (compartmentsField == null) compartmentsField = findFieldByName(invClass, "compartments", List.class);

        // 1.21 Check for Equipment Field and EnumMap
        if (equipmentField == null) {
            equipmentField = findFieldByName(invClass, "equipment", null);
        }
        
        if (equipmentField != null && equipmentItemsField == null && armorField == null) {
            try {
                Object equipment = equipmentField.get(inventory);
                if (equipment != null) {
                    Class<?> eqClass = equipment.getClass();
                    for (Field f : eqClass.getDeclaredFields()) {
                        f.setAccessible(true);
                        if (EnumMap.class.isAssignableFrom(f.getType())) {
                             // Assuming the first EnumMap we find in PlayerEquipment is the items map
                             equipmentItemsField = f;
                             break;
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private Field findFieldWithValue(Object instance, Object value, Class<?> targetType) {
        for (Field f : findFieldsAssignable(instance.getClass(), targetType)) {
            try {
                if (f.get(instance) == value) return f;
            } catch (Exception ignored) {}
        }
        return null;
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

    private record ListField(Field field, int size) {}

    record InventoryLists(Object items, Object armor, Object offhand) {}
}