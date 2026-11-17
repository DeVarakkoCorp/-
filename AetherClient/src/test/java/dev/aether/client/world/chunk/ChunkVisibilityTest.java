package dev.aether.client.world.chunk;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChunkVisibilityTest {
    @Test
    void selectsLoadedChunksAroundFlooredPlayerPositionAcrossNegativeCoordinates() {
        Map<ChunkPos, ChunkSection> sections = sections(
                new ChunkPos(-2, 0, -1), new ChunkPos(-1, 0, -1), new ChunkPos(0, 0, -1),
                new ChunkPos(-2, 1, -1), new ChunkPos(1, 0, -1));

        var visible = ChunkVisibility.withinRadius(sections, -1, -1, 1);

        assertEquals(4, visible.size());
        assertTrue(visible.contains(new ChunkPos(-2, 0, -1)));
        assertTrue(visible.contains(new ChunkPos(-2, 1, -1)));
        assertFalse(visible.contains(new ChunkPos(1, 0, -1)));
    }

    @Test
    void radiusZeroSelectsOnlyThePlayerChunkColumn() {
        Map<ChunkPos, ChunkSection> sections = sections(
                new ChunkPos(0, 0, 0), new ChunkPos(0, 1, 0), new ChunkPos(1, 0, 0));

        var visible = ChunkVisibility.withinRadius(sections, 15, 0, 0);

        assertEquals(2, visible.size());
        assertTrue(visible.contains(new ChunkPos(0, 0, 0)));
        assertTrue(visible.contains(new ChunkPos(0, 1, 0)));
    }

    @Test
    void rejectsUnboundedVisibilityRequests() {
        assertThrows(IllegalArgumentException.class,
                () -> ChunkVisibility.withinRadius(Map.of(), 0, 0, -1));
        assertThrows(IllegalArgumentException.class,
                () -> ChunkVisibility.withinRadius(Map.of(), 0, 0, 33));
        assertThrows(NullPointerException.class,
                () -> ChunkVisibility.withinRadius(null, 0, 0, 1));
    }

    @Test
    void createsBoundedSquareOfGenerationCandidates() {
        var positions = ChunkVisibility.columnsAround(-1, -1, 1);

        assertEquals(9, positions.size());
        assertTrue(positions.contains(new ChunkPos(-1, 0, -1)));
        assertTrue(positions.contains(new ChunkPos(-2, 0, -2)));
        assertTrue(positions.contains(new ChunkPos(0, 0, 0)));
    }

    private static Map<ChunkPos, ChunkSection> sections(ChunkPos... positions) {
        Map<ChunkPos, ChunkSection> sections = new HashMap<>();
        for (ChunkPos position : positions) sections.put(position, new ChunkSection());
        return sections;
    }
}
