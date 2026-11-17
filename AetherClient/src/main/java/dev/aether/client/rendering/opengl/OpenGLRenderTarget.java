package dev.aether.client.rendering.opengl;

import dev.aether.client.rendering.RenderException;
import dev.aether.client.rendering.RenderTarget;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public final class OpenGLRenderTarget implements RenderTarget {
    private final Thread ownerThread = Thread.currentThread();

    private int framebuffer;
    private int colorTexture;
    private int depthTexture;
    private int width;
    private int height;
    private boolean closed;

    public OpenGLRenderTarget(int width, int height) {
        RenderTarget.validateDimensions(width, height);
        TargetObjects objects = createTarget(width, height);
        this.framebuffer = objects.framebuffer;
        this.colorTexture = objects.colorTexture;
        this.depthTexture = objects.depthTexture;
        this.width = width;
        this.height = height;
    }

    private record TargetObjects(int framebuffer, int colorTexture, int depthTexture) {}

    private static TargetObjects createTarget(int width, int height) {
        int oldDrawFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int oldReadFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);
        int oldTexture = glGetInteger(GL_TEXTURE_BINDING_2D);
        int fbo = 0;
        int color = 0;
        int depth = 0;
        try {
            fbo = glGenFramebuffers();
            if (fbo == 0) throw new RenderException("Could not create framebuffer");
            glBindFramebuffer(GL_FRAMEBUFFER, fbo);

            color = glGenTextures();
            if (color == 0) throw new RenderException("Could not create color texture");
            glBindTexture(GL_TEXTURE_2D, color);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, color, 0);

            depth = glGenTextures();
            if (depth == 0) throw new RenderException("Could not create depth texture");
            glBindTexture(GL_TEXTURE_2D, depth);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0,
                    GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, 0L);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depth, 0);

            int status = glCheckFramebufferStatus(GL_FRAMEBUFFER);
            if (status != GL_FRAMEBUFFER_COMPLETE) {
                throw new RenderException("Framebuffer incomplete: 0x" + Integer.toHexString(status));
            }
            return new TargetObjects(fbo, color, depth);
        } catch (RuntimeException | Error failure) {
            if (depth != 0) glDeleteTextures(depth);
            if (color != 0) glDeleteTextures(color);
            if (fbo != 0) glDeleteFramebuffers(fbo);
            throw failure;
        } finally {
            glBindTexture(GL_TEXTURE_2D, oldTexture);
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, oldDrawFramebuffer);
            glBindFramebuffer(GL_READ_FRAMEBUFFER, oldReadFramebuffer);
        }
    }

    private void checkThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("Render target used from a non-context thread");
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Render target is closed");
    }

    @Override
    public void bind() {
        ensureOpen();
        checkThread();
        glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
        glViewport(0, 0, width, height);
    }

    @Override
    public void unbind() {
        ensureOpen();
        checkThread();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    @Override
    public int width() { return width; }

    @Override
    public int height() { return height; }

    public int colorTextureId() {
        ensureOpen();
        checkThread();
        return colorTexture;
    }

    @Override
    public void resize(int newWidth, int newHeight) {
        ensureOpen();
        checkThread();
        RenderTarget.validateDimensions(newWidth, newHeight);
        if (newWidth == width && newHeight == height) return;

        TargetObjects replacement = createTarget(newWidth, newHeight);
        int oldFramebuffer = framebuffer;
        int oldColor = colorTexture;
        int oldDepth = depthTexture;
        int oldDrawFramebuffer = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int oldReadFramebuffer = glGetInteger(GL_READ_FRAMEBUFFER_BINDING);

        framebuffer = replacement.framebuffer;
        colorTexture = replacement.colorTexture;
        depthTexture = replacement.depthTexture;
        width = newWidth;
        height = newHeight;

        if (oldDrawFramebuffer == oldFramebuffer) glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
        if (oldReadFramebuffer == oldFramebuffer) glBindFramebuffer(GL_READ_FRAMEBUFFER, framebuffer);

        glDeleteTextures(oldDepth);
        glDeleteTextures(oldColor);
        glDeleteFramebuffers(oldFramebuffer);
    }

    @Override
    public void close() {
        checkThread();
        if (closed) return;
        closed = true;
        if (glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING) == framebuffer) {
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER, 0);
        }
        if (glGetInteger(GL_READ_FRAMEBUFFER_BINDING) == framebuffer) {
            glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
        }
        if (depthTexture != 0) glDeleteTextures(depthTexture);
        if (colorTexture != 0) glDeleteTextures(colorTexture);
        if (framebuffer != 0) glDeleteFramebuffers(framebuffer);
        depthTexture = colorTexture = framebuffer = 0;
    }
}
