package dev.aether.client.world;

import dev.aether.client.world.chunk.ChunkWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlockLightEngineTest {
    private final BlockRegistry blocks = BlockRegistry.builder()
            .register(new BlockDefinition(1, RenderLayer.OPAQUE, true, true))
            .register(new BlockDefinition(2, RenderLayer.OPAQUE, true, true, 5))
            .build();

    @Test
    void lightFallsByOnePerAirBlock() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(8, 8, 8, 2);

        LightMap light = new BlockLightEngine().compute(world, blocks);

        assertEquals(5, light.levelAt(8, 8, 8));
        assertEquals(4, light.levelAt(9, 8, 8));
        assertEquals(3, light.levelAt(10, 8, 8));
        assertEquals(1, light.levelAt(12, 8, 8));
        assertEquals(0, light.levelAt(13, 8, 8));
    }

    @Test
    void opaqueBlocksDoNotReceivePropagatedLight() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(7, 8, 8, 2);
        world.setBlock(8, 8, 8, 1);

        LightMap light = new BlockLightEngine().compute(world, blocks);

        assertEquals(5, light.levelAt(7, 8, 8));
        assertEquals(0, light.levelAt(8, 8, 8));
    }

    @Test
    void lightPropagatesAcrossLoadedChunkBoundariesIncludingNegativeCoordinates() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(-1, 8, 8, 2);
        world.getOrCreate(dev.aether.client.world.chunk.ChunkPos.fromBlock(0, 8, 8));

        LightMap light = new BlockLightEngine().compute(world, blocks);

        assertEquals(5, light.levelAt(-1, 8, 8));
        assertEquals(4, light.levelAt(0, 8, 8));
        assertEquals(3, light.levelAt(1, 8, 8));
    }

    @Test
    void returnsAnEmptyMapWhenNoBlocksEmitLight() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(8, 8, 8, 1);

        LightMap light = new BlockLightEngine().compute(world, blocks);

        assertEquals(0, light.litBlockCount());
        assertEquals(0, light.levelAt(8, 8, 8));
    }

    @Test
    void rejectsEmissionOutsideTheSupportedLightRange() {
        assertThrows(IllegalArgumentException.class,
                () -> new BlockDefinition(3, RenderLayer.OPAQUE, true, true, -1));
        assertThrows(IllegalArgumentException.class,
                () -> new BlockDefinition(3, RenderLayer.OPAQUE, true, true, 16));
    }
}
