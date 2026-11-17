package dev.aether.client.game;

import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockPos;
import dev.aether.client.world.RenderLayer;
import dev.aether.client.world.chunk.ChunkWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockRaycasterTest {
    private static final BlockRegistry BLOCKS = BlockRegistry.builder()
            .register(new BlockDefinition(1, RenderLayer.OPAQUE, true, true))
            .register(new BlockDefinition(2, RenderLayer.TRANSLUCENT, false, false))
            .build();
    private final BlockRaycaster raycaster = new BlockRaycaster();

    @Test
    void hitsNearestCollidableBlockAndReturnsEntryFaceAndPlacementCell() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(3, 1, 0, 1);
        world.setBlock(5, 1, 0, 1);

        BlockRaycaster.Hit hit = raycaster.cast(world, BLOCKS,
                0.5, 1.5, 0.5, 10.0, 0.0, 0.0, 4.0).orElseThrow();

        assertEquals(new BlockPos(3, 1, 0), hit.block());
        assertEquals(new BlockPos(2, 1, 0), hit.adjacent());
        assertEquals(BlockRaycaster.Face.WEST, hit.face());
        assertEquals(2.5, hit.distance(), 1.0e-9);
        assertEquals(1, hit.blockId());
    }

    @Test
    void traversesNegativeCoordinatesAndReportsCorrectFace() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(-2, 0, 0, 1);

        BlockRaycaster.Hit hit = raycaster.cast(world, BLOCKS,
                0.5, 0.5, 0.5, -1.0, 0.0, 0.0, 4.0).orElseThrow();

        assertEquals(new BlockPos(-2, 0, 0), hit.block());
        assertEquals(new BlockPos(-1, 0, 0), hit.adjacent());
        assertEquals(BlockRaycaster.Face.EAST, hit.face());
        assertEquals(1.5, hit.distance(), 1.0e-9);
    }

    @Test
    void skipsNonCollidableBlocksAndHonorsReach() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(1, 0, 0, 2);
        world.setBlock(3, 0, 0, 1);

        assertTrue(raycaster.cast(world, BLOCKS, 0.5, 0.5, 0.5,
                1.0, 0.0, 0.0, 2.0).isEmpty());
        BlockRaycaster.Hit hit = raycaster.cast(world, BLOCKS, 0.5, 0.5, 0.5,
                1.0, 0.0, 0.0, 3.0).orElseThrow();
        assertEquals(new BlockPos(3, 0, 0), hit.block());
    }

    @Test
    void rejectsInvalidRayValuesAndZeroDirection() {
        ChunkWorld world = new ChunkWorld();
        assertThrows(IllegalArgumentException.class, () -> raycaster.cast(world, BLOCKS,
                0, 0, 0, 0, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> raycaster.cast(world, BLOCKS,
                Double.NaN, 0, 0, 1, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> raycaster.cast(world, BLOCKS,
                0, 0, 0, 1, 0, 0, 0));
    }

    @Test
    void raysStartingInsideBlocksReturnAnInsideHit() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(0, 0, 0, 1);

        BlockRaycaster.Hit hit = raycaster.cast(world, BLOCKS,
                0.5, 0.5, 0.5, 0, 0, 1, 3).orElseThrow();

        assertEquals(BlockRaycaster.Face.INSIDE, hit.face());
        assertEquals(0.0, hit.distance());
    }

    @Test
    void simultaneousAxisCrossingDoesNotHitSideCellsTouchedOnlyAtCorner() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(1, 0, 0, 1);

        assertTrue(raycaster.cast(world, BLOCKS,
                0.5, 0.5, 0.5, 1.0, 1.0, 0.0, 4.0).isEmpty());
    }
}
