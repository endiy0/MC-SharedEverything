# Verification Steps for Duplication Bug Fix

## Issue
Items were duplicating in Armor and Offhand slots when rapidly clicking (putting in and taking out) due to a race condition where the plugin would overwrite the shared inventory with an older or redundant state from the player's client view, effectively "restoring" an item that had just been removed.

## Fix
Modified `SharedInventoryManager.java` to prevent redundant updates. The `refreshViewers` method now checks if the Armor or Offhand contents in the shared inventory are effectively the same as the source player's inventory before applying any changes.

## Verification Procedure
1. **Setup**:
   - Start the server with the updated plugin.
   - Join with two players (Player A and Player B) in Shared or Team inventory mode.
   - Obtain a set of armor (e.g., Diamond Chestplate) and an item for the offhand (e.g., Shield).

2. **Test 1: Rapid Armor Equipping (Player A)**
   - Open inventory.
   - Rapidly click to equip and unequip the Chestplate from the armor slot.
   - **Expected**: The item should simply move between the cursor/inventory and the armor slot.
   - **Fail**: If the item duplicates (one on cursor, one stays in slot) or disappears.

3. **Test 2: Offhand Swapping (Player A)**
   - Put a Shield in the hotbar.
   - Rapidly press 'F' (Swap Hands) to move the Shield between Main Hand and Offhand.
   - **Expected**: The item swaps back and forth cleanly.
   - **Fail**: If a ghost item appears or a real item is duplicated.

4. **Test 3: Viewer Sync (Player B)**
   - Have Player B watch Player A.
   - Player A performs the rapid actions above.
   - **Expected**: Player B sees the changes reflect accurately (though maybe with slight visual lag due to tick rate, but no permanent duplication).

5. **Test 4: Unlinked State (Sanity Check)**
   - (Optional) If possible, disable NMS linking or test on a version where it falls back (1.21.1+).
   - Verify that updates still propagate (i.e., when Player A changes armor, Player B eventually sees it).
