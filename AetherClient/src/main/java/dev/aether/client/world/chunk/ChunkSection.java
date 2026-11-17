package dev.aether.client.world.chunk;

import dev.aether.client.world.BlockDefinition;

import java.util.Arrays;

public final class ChunkSection {
    public static final int EDGE = 16;
    public static final int VOLUME = EDGE * EDGE * EDGE;

    private final int[] blockIds = new int[VOLUME];
    private int nonAirCount;

    public synchronized int getBlockId(int x, int y, int z) {
        return blockIds[index(x, y, z)];
    }

    public synchronized void setBlockId(int x, int y, int z, int blockId) {
        if (blockId < 0) throw new IllegalArgumentException("Block id must not be negative");
        int index = index(x, y, z);
        int previous = blockIds[index];
        if (previous == blockId) return;
        if (previous == BlockDefinition.AIR_ID) nonAirCount++;
        if (blockId == BlockDefinition.AIR_ID) nonAirCount--;
        blockIds[index] = blockId;
    }

    public synchronized int nonAirCount() {
        return nonAirCount;
    }

    public synchronized boolean isEmpty() {
        return nonAirCount == 0;
    }

    public synchronized int[] copyBlockIds() {
        return Arrays.copyOf(blockIds, blockIds.length);
    }

    public synchronized ChunkSection snapshot() {
        ChunkSection copy = new ChunkSection();
        System.arraycopy(blockIds, 0, copy.blockIds, 0, blockIds.length);
        copy.nonAirCount = nonAirCount;
        return copy;
    }

    private static int index(int x, int y, int z) {
        if ((x | y | z) < 0 || x >= EDGE || y >= EDGE || z >= EDGE) {
            throw new IndexOutOfBoundsException("Block coordinates must be within 0..15");
        }
        return (y << 8) | (z << 4) | x;
    }
}
