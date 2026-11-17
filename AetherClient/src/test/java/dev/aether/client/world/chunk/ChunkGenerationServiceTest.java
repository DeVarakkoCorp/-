package dev.aether.client.world.chunk;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChunkGenerationServiceTest {
    @Test
    void generatesASectionOnAWorkerAndReturnsCurrentResult() throws Exception {
        ChunkGenerationService service = new ChunkGenerationService(1, 2);
        try {
            ChunkGenerationService.GeneratedChunk result = service.submit(new ChunkPos(1, 0, -1), 123)
                    .get(5, TimeUnit.SECONDS);

            assertEquals(new ChunkPos(1, 0, -1), result.position());
            assertFalse(result.section().isEmpty());
            assertTrue(service.isCurrent(result));
        } finally {
            service.close();
        }
    }

    @Test
    void invalidationMakesACompletedGenerationResultStale() throws Exception {
        ChunkGenerationService service = new ChunkGenerationService(1, 2);
        ChunkPos position = new ChunkPos(0, 0, 0);
        try {
            ChunkGenerationService.GeneratedChunk result = service.submit(position, 1)
                    .get(5, TimeUnit.SECONDS);
            service.invalidate(position);

            assertFalse(service.isCurrent(result));
        } finally {
            service.close();
        }
    }

    @Test
    void retiringCompletedRequestsBoundsTrackingAndKeepsNewRevisionsDistinct() throws Exception {
        ChunkGenerationService service = new ChunkGenerationService(1, 2);
        ChunkPos position = new ChunkPos(0, 0, 0);
        try {
            ChunkGenerationService.GeneratedChunk first = service.submit(position, 1)
                    .get(5, TimeUnit.SECONDS);
            service.retire(position, first.revision());
            assertFalse(service.isCurrent(first));

            ChunkGenerationService.GeneratedChunk second = service.submit(position, 1)
                    .get(5, TimeUnit.SECONDS);
            assertNotEquals(first.revision(), second.revision());
            assertTrue(service.isCurrent(second));
        } finally {
            service.close();
        }
    }

    @Test
    void rejectsInvalidPoolSizesBoundsAndSubmissionsAfterClose() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkGenerationService(0, 1));
        assertThrows(IllegalArgumentException.class, () -> new ChunkGenerationService(1, 0));
        ChunkGenerationService service = new ChunkGenerationService(1, 1);
        assertThrows(IllegalArgumentException.class,
                () -> service.submit(new ChunkPos(9, 0, 0), 1));
        service.close();
        assertThrows(IllegalStateException.class,
                () -> service.submit(new ChunkPos(0, 0, 0), 1));
    }
}
