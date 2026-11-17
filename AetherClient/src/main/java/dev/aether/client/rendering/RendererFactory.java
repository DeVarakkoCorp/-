package dev.aether.client.rendering;

import dev.aether.client.world.chunk.ChunkStore;
import dev.aether.client.world.chunk.ChunkWorld;

public interface RendererFactory {
    ClientRenderer create(ChunkWorld world, ChunkStore chunkStore);
}
