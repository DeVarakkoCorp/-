package dev.aether.client.world.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DemoWorldGeneratorTest {
    @Test
    void generatesContiguousNineSectionStarterWorld() {
        ChunkWorld world = DemoWorldGenerator.create(1);
        assertEquals(9, world.loadedSectionCount());
        assertEquals(1, world.blockIdAt(0, 0, 0));
        assertEquals(1, world.blockIdAt(16, 0, 0));
        assertEquals(0, world.blockIdAt(48, 0, 0));
    }

    @Test
    void rejectsUnboundedGenerationRadius() {
        assertThrows(IllegalArgumentException.class, () -> DemoWorldGenerator.create(-1));
        assertThrows(IllegalArgumentException.class, () -> DemoWorldGenerator.create(9));
    }

    @Test
    void generatesIndividualBoundedChunksDeterministically() {
        ChunkPos position = new ChunkPos(-2, 0, 3);

        ChunkSection first = DemoWorldGenerator.generateSection(position, 42);
        ChunkSection second = DemoWorldGenerator.generateSection(position, 42);

        assertArrayEquals(first.copyBlockIds(), second.copyBlockIds());
        assertFalse(first.isEmpty());
        assertFalse(DemoWorldGenerator.canGenerate(new ChunkPos(9, 0, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> DemoWorldGenerator.generateSection(new ChunkPos(9, 0, 0), 42));
    }
}
