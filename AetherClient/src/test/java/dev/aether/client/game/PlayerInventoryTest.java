package dev.aether.client.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerInventoryTest {
    @Test
    void mergesItemsAndFillsAdditionalStacksInOrder() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.setSlot(0, new ItemStack(1, 63));

        assertEquals(0, inventory.addItems(1, 66));
        assertEquals(new ItemStack(1, 64), inventory.slot(0));
        assertEquals(new ItemStack(1, 64), inventory.slot(1));
        assertEquals(new ItemStack(1, 1), inventory.slot(2));
    }

    @Test
    void reportsCapacityAndRemovesFromOnlyTheSelectedHotbarStack() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.setSlot(0, new ItemStack(1, 2));
        inventory.selectHotbarSlot(1);
        inventory.setSlot(1, new ItemStack(2, 3));

        assertTrue(inventory.canAddItems(1, 1));
        assertTrue(inventory.removeFromSelected(2));
        assertEquals(new ItemStack(2, 1), inventory.selectedStack());
        assertEquals(new ItemStack(1, 2), inventory.slot(0));
        assertThrows(IllegalArgumentException.class, () -> inventory.selectHotbarSlot(9));
    }

    @Test
    void refusesOverflowAndRejectsInvalidStacks() {
        PlayerInventory inventory = new PlayerInventory();
        for (int slot = 0; slot < PlayerInventory.SLOT_COUNT; slot++) {
            inventory.setSlot(slot, new ItemStack(2, ItemStack.MAX_STACK_SIZE));
        }

        assertFalse(inventory.canAddItems(1, 1));
        assertEquals(1, inventory.addItems(1, 1));
        assertThrows(IllegalArgumentException.class, () -> new ItemStack(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ItemStack(1, 65));
    }

    @Test
    void swapsSlotsWithoutChangingSelectedHotbarIndex() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.setSlot(0, new ItemStack(1, 8));
        inventory.setSlot(18, new ItemStack(2, 3));
        inventory.selectHotbarSlot(0);

        inventory.swapSlots(0, 18);

        assertEquals(new ItemStack(2, 3), inventory.slot(0));
        assertEquals(new ItemStack(1, 8), inventory.slot(18));
        assertEquals(0, inventory.selectedHotbarSlot());
    }
}
