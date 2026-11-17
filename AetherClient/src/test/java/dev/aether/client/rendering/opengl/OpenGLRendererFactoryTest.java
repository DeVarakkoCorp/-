package dev.aether.client.rendering.opengl;

import dev.aether.client.rendering.ClientRenderer;
import dev.aether.client.world.chunk.ChunkStore;
import dev.aether.client.world.chunk.ChunkWorld;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class OpenGLRendererFactoryTest {
    @TempDir
    Path directory;

    @Test
    void createsTheOpenGLBackendThroughTheNeutralRendererContract() {
        ClientRenderer renderer = new OpenGLRendererFactory()
                .create(new ChunkWorld(), new ChunkStore(directory));
        assertInstanceOf(OpenGLRenderer.class, renderer);
        renderer.close();
    }

    @Test
    void rejectsMissingWorldOrChunkStore() {
        OpenGLRendererFactory factory = new OpenGLRendererFactory();
        assertThrows(NullPointerException.class, () -> factory.create(null, new ChunkStore(directory)));
        assertThrows(NullPointerException.class, () -> factory.create(new ChunkWorld(), null));
    }
}
