package dev.aether.client.world.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkWorldTest {
    @Test
    void revisionChangesOnlyWhenWorldContentsChange() {
        ChunkWorld world = new ChunkWorld();
        assertEquals(0L, world.revision());

        world.setBlock(2, 3, 4, 1);
        long afterPlacement = world.revision();
        assertEquals(1L, afterPlacement);
        world.setBlock(2, 3, 4, 1);
        assertEquals(afterPlacement, world.revision());
        world.setBlock(2, 3, 4, 0);
        assertEquals(afterPlacement + 1, world.revision());
        world.setBlock(2, 3, 4, 0);
        assertEquals(afterPlacement + 1, world.revision());
    }

    @Test
    void mapsPositiveAndNegativeBlockCoordinatesToSections() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(-1, 0, 16, 4);
        assertEquals(4, world.blockIdAt(-1, 0, 16));
        assertEquals(0, world.blockIdAt(-2, 0, 16));
        assertEquals(1, world.loadedSectionCount());
        assertEquals(new ChunkPos(-1, 0, 1), ChunkPos.fromBlock(-1, 0, 16));
    }

    @Test
    void unloadedSectionsReadAsAirAndCanBeUnloaded() {
        ChunkWorld world = new ChunkWorld();
        ChunkPos position = new ChunkPos(1, 2, 3);
        assertEquals(0, world.blockIdAt(16, 32, 48));
        world.setBlock(16, 32, 48, 8);
        assertEquals(1, world.loadedSectionCount());
        assertTrue(world.unload(position));
        assertFalse(world.unload(position));
        assertEquals(0, world.blockIdAt(16, 32, 48));
    }

    @Test
    void worldSnapshotDoesNotObserveLaterEdits() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(16, 32, 48, 8);
        var snapshot = world.snapshot();
        world.setBlock(16, 32, 48, 9);
        assertEquals(8, snapshot.blockIdAt(16, 32, 48));
        assertEquals(9, world.blockIdAt(16, 32, 48));
    }
}
