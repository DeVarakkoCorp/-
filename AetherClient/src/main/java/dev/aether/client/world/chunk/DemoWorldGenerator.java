package dev.aether.client.world.chunk;

public final class DemoWorldGenerator {
    public static final int MAX_RADIUS_IN_CHUNKS = 8;

    private DemoWorldGenerator() {}

    public static ChunkWorld create(int radiusInChunks) {
        return create(radiusInChunks, 1);
    }

    public static ChunkWorld create(int radiusInChunks, int seed) {
        if (radiusInChunks < 0 || radiusInChunks > MAX_RADIUS_IN_CHUNKS) {
            throw new IllegalArgumentException("Demo world radius must be between 0 and 8 chunks");
        }
        ChunkWorld world = new ChunkWorld();
        for (int chunkZ = -radiusInChunks; chunkZ <= radiusInChunks; chunkZ++) {
            for (int chunkX = -radiusInChunks; chunkX <= radiusInChunks; chunkX++) {
                ChunkPos position = new ChunkPos(chunkX, 0, chunkZ);
                world.putSection(position, generateSection(position, seed));
            }
        }
        return world;
    }

    public static boolean canGenerate(ChunkPos position) {
        return position.y() == 0 && Math.abs((long) position.x()) <= MAX_RADIUS_IN_CHUNKS
                && Math.abs((long) position.z()) <= MAX_RADIUS_IN_CHUNKS;
    }

    public static ChunkSection generateSection(ChunkPos position, int seed) {
        if (position == null) throw new NullPointerException("position");
        if (!canGenerate(position)) throw new IllegalArgumentException("Chunk is outside the demo generator bounds");
        ChunkSection section = new ChunkSection();
        int phaseSeed = seed - 1;
        double phaseX = (phaseSeed & 0xffff) * 0.001;
        double phaseZ = ((phaseSeed >>> 16) & 0xffff) * 0.001;
        for (int z = 0; z < ChunkSection.EDGE; z++) {
            for (int x = 0; x < ChunkSection.EDGE; x++) {
                int worldX = position.originX() + x;
                int worldZ = position.originZ() + z;
                int height = 3 + (int) Math.round(1.5 * Math.sin((worldX + phaseX) * 0.18)
                        * Math.cos((worldZ + phaseZ) * 0.16));
                for (int y = 0; y <= height; y++) section.setBlockId(x, y, z, 1);
            }
        }
        return section;
    }
}
