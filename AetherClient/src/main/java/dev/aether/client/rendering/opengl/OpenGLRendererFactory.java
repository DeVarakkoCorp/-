package dev.aether.client.rendering.opengl;

import dev.aether.client.rendering.ClientRenderer;
import dev.aether.client.rendering.RendererFactory;
import dev.aether.client.world.chunk.ChunkStore;
import dev.aether.client.world.chunk.ChunkWorld;

import java.util.Objects;

public final class OpenGLRendererFactory implements RendererFactory {
    @Override
    public ClientRenderer create(ChunkWorld world, ChunkStore chunkStore) {
        return new OpenGLRenderer(Objects.requireNonNull(world, "world"),
                Objects.requireNonNull(chunkStore, "chunkStore"));
    }
}
