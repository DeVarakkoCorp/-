package dev.aether.client.rendering;

public final class RenderException extends RuntimeException {
    public RenderException(String message) {
        super(message);
    }

    public RenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
