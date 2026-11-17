package dev.aether.client.world.chunk;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class ChunkStreamingControllerTest {
    @Test
    void fillsBoundedWindowAsynchronouslyAndDoesNotDuplicateRequests() {
        ChunkWorld world = new ChunkWorld();
        try (ChunkStreamingController streaming = new ChunkStreamingController(world, 2, 16, 1)) {
            streaming.updateAround(8, 8, 1);
            assertEquals(9, streaming.pendingCount() + world.loadedSectionCount());
            streaming.updateAround(8, 8, 1);
            assertEquals(9, streaming.pendingCount() + world.loadedSectionCount());

            await(() -> {
                streaming.updateAround(8, 8, 1);
                return world.loadedSectionCount() == 9;
            });

            assertEquals(9, world.loadedSectionCount());
            assertEquals(0, streaming.pendingCount());
            assertTrue(streaming.desiredChunks().contains(new ChunkPos(0, 0, 0)));
        }
    }

    @Test
    void movingTheWindowRequestsOnlyMissingChunksAndKeepsEarlierChunksLoaded() {
        ChunkWorld world = new ChunkWorld();
        try (ChunkStreamingController streaming = new ChunkStreamingController(world, 2, 16, 1)) {
            await(() -> {
                streaming.updateAround(8, 8, 1);
                return world.loadedSectionCount() == 9;
            });
            streaming.updateAround(32, 8, 1);

            await(() -> {
                streaming.updateAround(32, 8, 1);
                return world.loadedSectionCount() == 15;
            });

            assertEquals(15, world.loadedSectionCount());
            assertTrue(streaming.desiredChunks().contains(new ChunkPos(3, 0, 0)));
            assertNotNull(world.get(new ChunkPos(-1, 0, 0)));
        }
    }

    @org.junit.jupiter.api.Test
    void persistentWindowUnloadsSectionsAndRestoresEditsWhenReturning(
            @org.junit.jupiter.api.io.TempDir Path directory) throws IOException {
        ChunkWorld world = new ChunkWorld();
        ChunkStore store = new ChunkStore(directory);
        try (ChunkStreamingController streaming = new ChunkStreamingController(world, 2, 16, 1, store)) {
            await(() -> {
                streaming.updateAround(8, 8, 1);
                return world.loadedSectionCount() == 9;
            });
            world.setBlock(1, 10, 1, 7);

            await(() -> {
                streaming.updateAround(40, 8, 1);
                return world.loadedSectionCount() == 9 && world.get(new ChunkPos(0, 0, 0)) == null;
            });
            assertTrue(store.load(new ChunkPos(0, 0, 0)).isPresent());

            await(() -> {
                streaming.updateAround(8, 8, 1);
                return world.get(new ChunkPos(0, 0, 0)) != null && world.loadedSectionCount() == 9;
            });
            assertEquals(7, world.blockIdAt(1, 10, 1));
            assertEquals(9, world.loadedSectionCount());
        }
    }

    @Test
    void repeatedAsyncUnloadReloadCyclesPreserveEditedSections(
            @org.junit.jupiter.api.io.TempDir Path directory) {
        ChunkWorld world = DemoWorldGenerator.create(1);
        ChunkStore store = new ChunkStore(directory);
        try (ChunkStreamingController streaming = new ChunkStreamingController(world, 2, 16, 1, store)) {
            world.setBlock(1, 10, 1, 7);
            for (int cycle = 0; cycle < 20; cycle++) {
                int x = cycle % 2 == 0 ? 40 : 8;
                int expectedChunkX = cycle % 2 == 0 ? 2 : 0;
                await(() -> {
                    streaming.updateAround(x, 8, 0);
                    ChunkSection section = world.get(new ChunkPos(expectedChunkX, 0, 0));
                    return section != null && world.loadedSectionCount() == 1
                            && streaming.pendingCount() == 0;
                });
                if (expectedChunkX == 0) assertEquals(7, world.blockIdAt(1, 10, 1));
            }
            assertEquals("", streaming.storageError());
            assertEquals(1, world.loadedSectionCount());
        }
    }

    @Test
    void generatorBoundsKeepRequestsAndMemoryGrowthFinite() {
        ChunkWorld world = new ChunkWorld();
        try (ChunkStreamingController streaming = new ChunkStreamingController(world, 1, 4, 1)) {
            streaming.updateAround(160, 0, 1);

            assertEquals(0, streaming.pendingCount());
            assertEquals(0, world.loadedSectionCount());
        }
    }

    @Test
    void reportsCorruptStoredChunkWithoutGeneratingOverIt(@org.junit.jupiter.api.io.TempDir Path directory)
            throws IOException {
        ChunkWorld world = new ChunkWorld();
        ChunkStore store = new ChunkStore(directory);
        Files.write(store.pathFor(new ChunkPos(0, 0, 0)), new byte[]{1, 2, 3});
        try (ChunkStreamingController streaming = new ChunkStreamingController(world, 1, 4, 1, store)) {
            await(() -> {
                streaming.updateAround(8, 8, 0);
                return !streaming.storageError().isEmpty();
            });
            assertTrue(streaming.storageError().contains("Could not load chunk"));
            assertNull(world.get(new ChunkPos(0, 0, 0)));
        }
    }

    @Test
    void keepsSectionsLoadedWhenAsynchronousPersistenceFails(@org.junit.jupiter.api.io.TempDir Path directory)
            throws IOException {
        Path notDirectory = directory.resolve("not-a-directory");
        Files.write(notDirectory, new byte[]{1});
        ChunkWorld world = DemoWorldGenerator.create(1);
        try (ChunkStreamingController streaming = new ChunkStreamingController(world, 1, 4, 1,
                new ChunkStore(notDirectory))) {
            await(() -> {
                streaming.updateAround(8, 8, 0);
                return !streaming.storageError().isEmpty();
            });
            assertTrue(streaming.storageError().contains("Could not persist chunk"));
            assertEquals(9, world.loadedSectionCount());
        }
    }

    @Test
    void rejectsInvalidWindowAndUpdatesAfterClose() {
        ChunkStreamingController streaming = new ChunkStreamingController(new ChunkWorld(), 1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> streaming.updateAround(0, 0, 33));
        streaming.close();
        assertThrows(IllegalStateException.class, () -> streaming.updateAround(0, 0, 1));
    }

    private static void await(Check check) {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            while (!check.done()) Thread.sleep(2);
        });
    }

    @FunctionalInterface
    private interface Check {
        boolean done() throws InterruptedException;
    }
}
