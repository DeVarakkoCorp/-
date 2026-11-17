package dev.aether.client.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HotbarLayoutTest {
    @Test
    void buildsNinePixelAlignedSlotsAndAddsMarkersForOccupiedItems() {
        boolean[] occupied = new boolean[HotbarLayout.SLOT_COUNT];
        float[] empty = HotbarLayout.vertices(1280, 720, 0, occupied);
        occupied[0] = true;
        occupied[8] = true;
        float[] withItems = HotbarLayout.vertices(1280, 720, 8, occupied);

        assertEquals((1 + 2 * HotbarLayout.SLOT_COUNT) * 6 * 5, empty.length);
        assertEquals(empty.length + 2 * 6 * 5, withItems.length);
        for (float component : withItems) assertTrue(Float.isFinite(component));
    }

    @Test
    void rejectsInvalidViewportSelectionAndOccupancy() {
        boolean[] occupied = new boolean[HotbarLayout.SLOT_COUNT];
        assertThrows(IllegalArgumentException.class, () -> HotbarLayout.vertices(0, 720, 0, occupied));
        assertThrows(IllegalArgumentException.class, () -> HotbarLayout.vertices(1280, 720, 9, occupied));
        assertThrows(IllegalArgumentException.class, () -> HotbarLayout.vertices(1280, 720, 0, new boolean[8]));
    }
}
