package dev.aether.client.rendering.opengl;

import dev.aether.client.rendering.RenderException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

/** A small snapshot of the active OpenGL device, queried with a current context. */
public record OpenGLCapabilities(
        String vendor,
        String renderer,
        String version,
        int majorVersion,
        int minorVersion,
        int maxTextureSize,
        int maxColorAttachments,
        int maxSamples
) {
    public OpenGLCapabilities {
        if (vendor == null || vendor.isBlank()) throw new IllegalArgumentException("vendor must not be blank");
        if (renderer == null || renderer.isBlank()) throw new IllegalArgumentException("renderer must not be blank");
        if (version == null || version.isBlank()) throw new IllegalArgumentException("version must not be blank");
        if (majorVersion < 1 || minorVersion < 0) throw new IllegalArgumentException("Invalid OpenGL version");
        if (maxTextureSize <= 0 || maxColorAttachments <= 0 || maxSamples < 0) {
            throw new IllegalArgumentException("Invalid OpenGL device limits");
        }
    }

    public static OpenGLCapabilities query() {
        String vendor = glGetString(GL_VENDOR);
        String renderer = glGetString(GL_RENDERER);
        String version = glGetString(GL_VERSION);
        if (vendor == null || renderer == null || version == null) {
            throw new RenderException("OpenGL capabilities require a current context");
        }
        return new OpenGLCapabilities(vendor, renderer, version,
                glGetInteger(GL_MAJOR_VERSION), glGetInteger(GL_MINOR_VERSION),
                glGetInteger(GL_MAX_TEXTURE_SIZE), glGetInteger(GL_MAX_COLOR_ATTACHMENTS),
                glGetInteger(GL_MAX_SAMPLES));
    }
}
