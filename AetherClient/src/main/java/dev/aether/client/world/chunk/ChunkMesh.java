package dev.aether.client.world.chunk;

import java.util.Arrays;

public final class ChunkMesh {
    public static final int FLOATS_PER_VERTEX = 7;

    private final float[] vertices;
    private final int[] indices;
    private final int faceCount;

    ChunkMesh(float[] vertices, int[] indices, int faceCount) {
        this.vertices = vertices;
        this.indices = indices;
        this.faceCount = faceCount;
    }

    public float[] copyVertices() {
        return Arrays.copyOf(vertices, vertices.length);
    }

    public int[] copyIndices() {
        return Arrays.copyOf(indices, indices.length);
    }

    public int vertexCount() {
        return vertices.length / FLOATS_PER_VERTEX;
    }

    public int indexCount() {
        return indices.length;
    }

    public int faceCount() {
        return faceCount;
    }

    public boolean isEmpty() {
        return faceCount == 0;
    }
}
