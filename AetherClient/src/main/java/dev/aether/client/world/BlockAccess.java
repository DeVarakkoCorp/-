package dev.aether.client.world;

@FunctionalInterface
public interface BlockAccess {
    int blockIdAt(int worldX, int worldY, int worldZ);
}
