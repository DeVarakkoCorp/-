package dev.aether.client.world.chunk;

import dev.aether.client.world.BlockAccess;
import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.LightMap;
import dev.aether.client.world.RenderLayer;

import java.util.Arrays;
import java.util.Objects;

public final class ChunkMesher {
    private static final Face[] FACES = {
            new Face(1, 0, 0, new float[]{1, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1}),
            new Face(-1, 0, 0, new float[]{0, 0, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0}),
            new Face(0, 1, 0, new float[]{0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0}),
            new Face(0, -1, 0, new float[]{0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1}),
            new Face(0, 0, 1, new float[]{1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 1}),
            new Face(0, 0, -1, new float[]{0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 0})
    };

    public ChunkMesh build(
            ChunkSection section,
            int originX,
            int originY,
            int originZ,
            BlockRegistry registry,
            BlockAccess neighboringBlocks,
            RenderLayer layer
    ) {
        return build(section, originX, originY, originZ, registry, neighboringBlocks, layer,
                LightMap.FULL_BRIGHT);
    }

    public ChunkMesh build(
            ChunkSection section,
            int originX,
            int originY,
            int originZ,
            BlockRegistry registry,
            BlockAccess neighboringBlocks,
            RenderLayer layer,
            LightMap lightMap
    ) {
        Objects.requireNonNull(section, "section");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(neighboringBlocks, "neighboringBlocks");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(lightMap, "lightMap");

        FloatBuilder vertices = new FloatBuilder();
        IntBuilder indices = new IntBuilder();
        int faces = 0;

        for (int y = 0; y < ChunkSection.EDGE; y++) {
            for (int z = 0; z < ChunkSection.EDGE; z++) {
                for (int x = 0; x < ChunkSection.EDGE; x++) {
                    int blockId = section.getBlockId(x, y, z);
                    if (blockId == BlockDefinition.AIR_ID) continue;
                    BlockDefinition block = registry.get(blockId);
                    if (block.renderLayer() != layer) continue;

                    for (Face face : FACES) {
                        int neighborId = neighborId(section, neighboringBlocks,
                                originX, originY, originZ,
                                x + face.dx, y + face.dy, z + face.dz);
                        if (isOccluded(block, registry.get(neighborId))) continue;
                        int light = lightMap.levelAt(originX + x + face.dx,
                                originY + y + face.dy, originZ + z + face.dz);
                        appendFace(vertices, indices, x, y, z, face, light);
                        faces++;
                    }
                }
            }
        }
        return new ChunkMesh(vertices.toArray(), indices.toArray(), faces);
    }

    private static int neighborId(
            ChunkSection section,
            BlockAccess neighbors,
            int originX,
            int originY,
            int originZ,
            int x,
            int y,
            int z
    ) {
        if (x >= 0 && x < ChunkSection.EDGE && y >= 0 && y < ChunkSection.EDGE
                && z >= 0 && z < ChunkSection.EDGE) {
            return section.getBlockId(x, y, z);
        }
        return neighbors.blockIdAt(originX + x, originY + y, originZ + z);
    }

    private static boolean isOccluded(BlockDefinition block, BlockDefinition neighbor) {
        if (neighbor.id() == BlockDefinition.AIR_ID) return false;
        if (neighbor.opaque()) return true;
        return block.renderLayer() == RenderLayer.TRANSLUCENT
                && block.id() == neighbor.id();
    }

    private static void appendFace(
            FloatBuilder vertices,
            IntBuilder indices,
            int x,
            int y,
            int z,
            Face face,
            int light
    ) {
        int firstVertex = vertices.size() / ChunkMesh.FLOATS_PER_VERTEX;
        for (int i = 0; i < 4; i++) {
            int offset = i * 3;
            vertices.add(x + face.corners[offset]);
            vertices.add(y + face.corners[offset + 1]);
            vertices.add(z + face.corners[offset + 2]);
            vertices.add(face.dx);
            vertices.add(face.dy);
            vertices.add(face.dz);
            vertices.add(light);
        }
        indices.add(firstVertex);
        indices.add(firstVertex + 1);
        indices.add(firstVertex + 2);
        indices.add(firstVertex);
        indices.add(firstVertex + 2);
        indices.add(firstVertex + 3);
    }

    private record Face(int dx, int dy, int dz, float[] corners) {}

    private static final class FloatBuilder {
        private float[] values = new float[256];
        private int size;

        void add(float value) {
            if (size == values.length) values = Arrays.copyOf(values, values.length * 2);
            values[size++] = value;
        }

        int size() {
            return size;
        }

        float[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }

    private static final class IntBuilder {
        private int[] values = new int[128];
        private int size;

        void add(int value) {
            if (size == values.length) values = Arrays.copyOf(values, values.length * 2);
            values[size++] = value;
        }

        int[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }
}
