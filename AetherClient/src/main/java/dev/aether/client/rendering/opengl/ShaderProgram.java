package dev.aether.client.rendering.opengl;

import dev.aether.client.rendering.RenderException;

import static org.lwjgl.opengl.GL20.*;

public final class ShaderProgram implements AutoCloseable {
    private final Thread ownerThread = Thread.currentThread();
    private final int programId;
    private boolean closed;

    public ShaderProgram(String vertexSource, String fragmentSource) {
        int vertex = 0;
        int fragment = 0;
        int program = 0;
        try {
            vertex = compile(GL_VERTEX_SHADER, vertexSource, "vertex");
            fragment = compile(GL_FRAGMENT_SHADER, fragmentSource, "fragment");
            program = glCreateProgram();
            if (program == 0) throw new RenderException("glCreateProgram returned 0");
            glAttachShader(program, vertex);
            glAttachShader(program, fragment);
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
                throw new RenderException("Program link failed: " + glGetProgramInfoLog(program));
            }
            programId = program;
        } catch (RuntimeException | Error failure) {
            if (program != 0) glDeleteProgram(program);
            throw failure;
        } finally {
            if (vertex != 0) glDeleteShader(vertex);
            if (fragment != 0) glDeleteShader(fragment);
        }
    }

    private static int compile(int type, String source, String label) {
        int shader = glCreateShader(type);
        if (shader == 0) throw new RenderException("Could not create " + label + " shader");
        try {
            glShaderSource(shader, source);
            glCompileShader(shader);
            if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
                throw new RenderException(label + " shader compilation failed: " + glGetShaderInfoLog(shader));
            }
            return shader;
        } catch (RuntimeException | Error failure) {
            glDeleteShader(shader);
            throw failure;
        }
    }

    public void bind() {
        checkThread();
        ensureOpen();
        glUseProgram(programId);
    }

    public int uniformLocation(String name) {
        checkThread();
        ensureOpen();
        return glGetUniformLocation(programId, name);
    }

    private void checkThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("Shader program must be used on its OpenGL context thread");
        }
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("Shader program is closed");
    }

    @Override
    public void close() {
        checkThread();
        if (!closed) {
            closed = true;
            glDeleteProgram(programId);
        }
    }
}
