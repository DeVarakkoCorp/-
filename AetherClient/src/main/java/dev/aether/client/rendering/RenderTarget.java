package dev.aether.client.rendering;

public interface RenderTarget extends AutoCloseable {
    static void validateDimensions(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Render target dimensions must be positive");
        }
    }

    void bind();
    void unbind();
    int width();
    int height();
    void resize(int width, int height);
    @Override
    void close();
}
