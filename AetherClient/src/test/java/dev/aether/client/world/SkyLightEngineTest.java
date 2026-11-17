package dev.aether.client.world;

import dev.aether.client.world.chunk.ChunkPos;
import dev.aether.client.world.chunk.ChunkWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SkyLightEngineTest {
    private final BlockRegistry blocks = BlockRegistry.builder()
            .register(new BlockDefinition(1, RenderLayer.OPAQUE, true, true))
            .register(new BlockDefinition(2, RenderLayer.TRANSLUCENT, false, false))
            .build();

    @Test
    void directSkyLightsOpenAirAndStopsAtOpaqueTerrain() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(8, 4, 8, 1);

        LightMap sky = new SkyLightEngine().compute(world, blocks);

        assertEquals(15, sky.levelAt(8, 15, 8));
        assertEquals(15, sky.levelAt(8, 5, 8));
        assertEquals(0, sky.levelAt(8, 4, 8));
        assertEquals(0, sky.levelAt(8, 3, 8));
    }

    @Test
    void directSkyContinuesThroughAdjacentLoadedVerticalSections() {
        ChunkWorld world = new ChunkWorld();
        world.getOrCreate(new ChunkPos(0, 0, 0));
        world.getOrCreate(new ChunkPos(0, 1, 0));

        LightMap sky = new SkyLightEngine().compute(world, blocks);

        assertEquals(15, sky.levelAt(7, 0, 9));
        assertEquals(15, sky.levelAt(7, 31, 9));
    }

    @Test
    void lightMapMaximumCombinesSkyAndBlockEmission() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(8, 8, 8, 1);
        BlockRegistry emittingBlocks = BlockRegistry.builder()
                .register(new BlockDefinition(1, RenderLayer.OPAQUE, true, true, 5))
                .build();
        LightMap block = new BlockLightEngine().compute(world, emittingBlocks);
        LightMap sky = new SkyLightEngine().compute(world, emittingBlocks);

        LightMap combined = LightMap.maximum(block, sky);

        assertEquals(5, combined.levelAt(8, 8, 8));
        assertEquals(15, combined.levelAt(8, 9, 8));
    }
}
