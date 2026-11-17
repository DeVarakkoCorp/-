package dev.aether.client.world.chunk;

import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.LightMap;
import dev.aether.client.world.RenderLayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChunkMesherTest {
    private static final int STONE = 1;
    private static final BlockRegistry REGISTRY = BlockRegistry.builder()
            .register(new BlockDefinition(STONE, RenderLayer.OPAQUE, true, true))
            .register(new BlockDefinition(2, RenderLayer.TRANSLUCENT, false, false))
            .build();
    private final ChunkMesher mesher = new ChunkMesher();

    @Test
    void emptySectionProducesEmptyMesh() {
        ChunkMesh mesh = mesh(new ChunkSection(), 0, 0, 0);
        assertTrue(mesh.isEmpty());
        assertEquals(0, mesh.vertexCount());
        assertEquals(0, mesh.indexCount());
    }

    @Test
    void singleBlockProducesSixOutwardFacingQuads() {
        ChunkSection section = new ChunkSection();
        section.setBlockId(1, 2, 3, STONE);

        ChunkMesh mesh = mesh(section, 0, 0, 0);

        assertEquals(6, mesh.faceCount());
        assertEquals(24, mesh.vertexCount());
        assertEquals(36, mesh.indexCount());
        float[] vertices = mesh.copyVertices();
        assertEquals(2.0f, vertices[0]);
        assertEquals(0.0f, vertices[4]);
        assertEquals(0.0f, vertices[5]);
        assertEquals(15.0f, vertices[6]);
    }

    @Test
    void storesSampledFaceLightInEveryMeshVertex() {
        ChunkSection section = new ChunkSection();
        section.setBlockId(1, 2, 3, STONE);

        ChunkMesh darkMesh = mesher.build(section, 0, 0, 0, REGISTRY,
                (x, y, z) -> 0, RenderLayer.OPAQUE, LightMap.DARK);
        ChunkMesh brightMesh = mesher.build(section, 0, 0, 0, REGISTRY,
                (x, y, z) -> 0, RenderLayer.OPAQUE, LightMap.FULL_BRIGHT);

        assertEquals(0.0f, darkMesh.copyVertices()[6]);
        assertEquals(15.0f, brightMesh.copyVertices()[6]);
    }

    @Test
    void hidesSharedFaceBetweenAdjacentOpaqueBlocks() {
        ChunkSection section = new ChunkSection();
        section.setBlockId(1, 1, 1, STONE);
        section.setBlockId(2, 1, 1, STONE);

        assertEquals(10, mesh(section, 0, 0, 0).faceCount());
    }

    @Test
    void hidesBoundaryFaceAgainstOpaqueNeighborSection() {
        ChunkSection section = new ChunkSection();
        section.setBlockId(15, 8, 8, STONE);

        ChunkMesh mesh = mesher.build(section, 0, 0, 0, REGISTRY,
                (x, y, z) -> x == 16 && y == 8 && z == 8 ? STONE : 0,
                RenderLayer.OPAQUE);

        assertEquals(5, mesh.faceCount());
    }

    @Test
    void rejectsUnknownBlockIdsInsteadOfSilentlyCorruptingMesh() {
        ChunkSection section = new ChunkSection();
        section.setBlockId(0, 0, 0, 900);
        assertThrows(IllegalArgumentException.class, () -> mesh(section, 0, 0, 0));
    }

    private ChunkMesh mesh(ChunkSection section, int originX, int originY, int originZ) {
        return mesher.build(section, originX, originY, originZ, REGISTRY, (x, y, z) -> 0,
                RenderLayer.OPAQUE);
    }
}
