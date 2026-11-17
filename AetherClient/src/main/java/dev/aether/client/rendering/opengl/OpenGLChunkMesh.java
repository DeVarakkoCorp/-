package dev.aether.client.rendering.opengl;

import dev.aether.client.world.chunk.ChunkMesh;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public final class OpenGLChunkMesh implements AutoCloseable {
    private final Thread ownerThread = Thread.currentThread();
    private final int vao;
    private final int vertexBuffer;
    private final int indexBuffer;
    private final int indexCount;
    private boolean closed;

    public OpenGLChunkMesh(ChunkMesh mesh, int originX, int originY, int originZ) {
        float[] vertices = mesh.copyVertices();
        for (int i = 0; i < vertices.length; i += ChunkMesh.FLOATS_PER_VERTEX) {
            vertices[i] += originX;
            vertices[i + 1] += originY;
            vertices[i + 2] += originZ;
        }
        int[] indices = mesh.copyIndices();
        indexCount = indices.length;
        vao = glGenVertexArrays();
        vertexBuffer = glGenBuffers();
        indexBuffer = glGenBuffers();
        if (vao == 0 || vertexBuffer == 0 || indexBuffer == 0) {
            if (indexBuffer != 0) glDeleteBuffers(indexBuffer);
            if (vertexBuffer != 0) glDeleteBuffers(vertexBuffer);
            if (vao != 0) glDeleteVertexArrays(vao);
            throw new IllegalStateException("Failed to allocate chunk mesh buffers");
        }

        FloatBuffer vertexData = MemoryUtil.memAllocFloat(vertices.length);
        IntBuffer indexData = null;
        try {
            indexData = MemoryUtil.memAllocInt(indices.length);
            vertexData.put(vertices).flip();
            indexData.put(indices).flip();

            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
            glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexData, GL_STATIC_DRAW);

            int stride = ChunkMesh.FLOATS_PER_VERTEX * Float.BYTES;
            glEnableVertexAttribArray(0);
            glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
            glEnableVertexAttribArray(1);
            glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
            glEnableVertexAttribArray(2);
            glVertexAttribPointer(2, 1, GL_FLOAT, false, stride, 6L * Float.BYTES);
            glBindVertexArray(0);
            glBindBuffer(GL_ARRAY_BUFFER, 0);
        } catch (RuntimeException | Error failure) {
            glDeleteBuffers(indexBuffer);
            glDeleteBuffers(vertexBuffer);
            glDeleteVertexArrays(vao);
            throw failure;
        } finally {
            MemoryUtil.memFree(indexData);
            MemoryUtil.memFree(vertexData);
        }
    }

    public void draw() {
        checkThread();
        if (closed) throw new IllegalStateException("Chunk mesh is closed");
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    private void checkThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("Chunk mesh must be used on its OpenGL context thread");
        }
    }

    @Override
    public void close() {
        checkThread();
        if (closed) return;
        closed = true;
        glDeleteBuffers(indexBuffer);
        glDeleteBuffers(vertexBuffer);
        glDeleteVertexArrays(vao);
    }
}
