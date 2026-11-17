package dev.aether.client.world.chunk;

public record ChunkPos(int x, int y, int z) {
    public static ChunkPos fromBlock(int blockX, int blockY, int blockZ) {
        return new ChunkPos(Math.floorDiv(blockX, ChunkSection.EDGE),
                Math.floorDiv(blockY, ChunkSection.EDGE),
                Math.floorDiv(blockZ, ChunkSection.EDGE));
    }

    public int originX() { return Math.multiplyExact(x, ChunkSection.EDGE); }
    public int originY() { return Math.multiplyExact(y, ChunkSection.EDGE); }
    public int originZ() { return Math.multiplyExact(z, ChunkSection.EDGE); }
}
