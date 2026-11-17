package dev.aether.client.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryLayoutTest {
    @Test
    void mapsCursorPositionsToGridSlotsWithHotbarOnBottomRow() {
        assertEquals(9, InventoryLayout.slotAt(448, 342, 1280, 720));
        assertEquals(0, InventoryLayout.slotAt(448, 486, 1280, 720));
        assertEquals(35, InventoryLayout.slotAt(832, 438, 1280, 720));
        assertEquals(-1, InventoryLayout.slotAt(470, 342, 1280, 720));
        assertEquals(-1, InventoryLayout.slotAt(20, 20, 1280, 720));
    }

    @Test
    void drawsAllThirtySixSlotsAndItemMarkers() {
        int[] ids = new int[36];
        int[] counts = new int[36];
        float[] empty = InventoryLayout.vertices(1280, 720, 0, ids, counts);
        ids[0] = 1;
        counts[0] = 64;
        float[] occupied = InventoryLayout.vertices(1280, 720, 0, ids, counts);

        assertEquals(83 * 6 * 5, empty.length);
        assertTrue(occupied.length > empty.length + 2 * 6 * 5);
        for (float component : occupied) assertTrue(Float.isFinite(component));
    }

    @Test
    void rejectsMismatchedInventoryArrays() {
        assertThrows(IllegalArgumentException.class,
                () -> InventoryLayout.vertices(1280, 720, 0, new int[35], new int[36]));
    }

    @Test
    void mapsCraftingInputsAndOutputAboveTheInventoryGrid() {
        assertEquals(0, InventoryLayout.craftingSlotAt(460, 239, 1280, 720));
        assertEquals(3, InventoryLayout.craftingSlotAt(508, 287, 1280, 720));
        assertTrue(InventoryLayout.craftingOutputAt(618, 250, 1280, 720));
        assertEquals(9, InventoryLayout.slotAt(448, 342, 1280, 720));
    }
}
