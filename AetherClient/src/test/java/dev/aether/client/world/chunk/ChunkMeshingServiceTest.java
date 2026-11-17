package dev.aether.client.world.chunk;

import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.RenderLayer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChunkMeshingServiceTest {
    private static final BlockRegistry BLOCKS = BlockRegistry.builder()
            .register(new BlockDefinition(1, RenderLayer.OPAQUE, true, true))
            .build();

    @Test
    void meshesSnapshotOffCallerThreadAndReportsCurrentRevision() {
        ChunkPos position = new ChunkPos(0, 0, 0);
        ChunkSection source = new ChunkSection();
        source.setBlockId(4, 4, 4, 1);
        try (ChunkMeshingService service = new ChunkMeshingService(1, 4)) {
            ChunkMeshingService.MeshResult result = service
                    .submit(position, source, BLOCKS, (x, y, z) -> 0, RenderLayer.OPAQUE).join();
            assertEquals(6, result.mesh().faceCount());
            assertTrue(service.isCurrent(result));
        }
    }

    @Test
    void discardsAnInFlightResultAfterInvalidation() throws Exception {
        ChunkPos position = new ChunkPos(0, 0, 0);
        ChunkSection source = new ChunkSection();
        source.setBlockId(0, 0, 0, 1);
        CountDownLatch enteredNeighborLookup = new CountDownLatch(1);
        CountDownLatch continueMeshing = new CountDownLatch(1);
        try (ChunkMeshingService service = new ChunkMeshingService(1, 2)) {
            var stale = service.submit(position, source, BLOCKS, (x, y, z) -> {
                enteredNeighborLookup.countDown();
                try {
                    if (!continueMeshing.await(3, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out waiting to resume meshing");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(interrupted);
                }
                return 0;
            }, RenderLayer.OPAQUE);

            assertTrue(enteredNeighborLookup.await(3, TimeUnit.SECONDS));
            service.invalidate(position);
            continueMeshing.countDown();
            assertThrows(CancellationException.class, stale::join);
        } finally {
            continueMeshing.countDown();
        }
    }

    @Test
    void rejectsInvalidPoolSizes() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkMeshingService(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ChunkMeshingService(1, 0));
    }
}
