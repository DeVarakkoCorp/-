package dev.aether.client.world.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkSectionTest {
    @Test
    void storesBlocksAndTracksNonAirCount() {
        ChunkSection section = new ChunkSection();
        assertTrue(section.isEmpty());
        section.setBlockId(15, 15, 15, 7);
        section.setBlockId(0, 0, 0, 2);
        assertEquals(2, section.nonAirCount());
        assertEquals(7, section.getBlockId(15, 15, 15));
        section.setBlockId(15, 15, 15, 3);
        assertEquals(2, section.nonAirCount());
        section.setBlockId(0, 0, 0, 0);
        assertEquals(1, section.nonAirCount());
    }

    @Test
    void rejectsInvalidCoordinatesAndIds() {
        ChunkSection section = new ChunkSection();
        assertThrows(IndexOutOfBoundsException.class, () -> section.getBlockId(16, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> section.getBlockId(0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> section.setBlockId(0, 0, 0, -1));
    }

    @Test
    void copiedStorageCannotMutateSection() {
        ChunkSection section = new ChunkSection();
        int[] copy = section.copyBlockIds();
        copy[0] = 99;
        assertEquals(0, section.getBlockId(0, 0, 0));
    }
}
