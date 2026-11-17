package dev.aether.client.world;

public record BlockPos(int x, int y, int z) {
    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(Math.addExact(x, dx), Math.addExact(y, dy), Math.addExact(z, dz));
    }
}
