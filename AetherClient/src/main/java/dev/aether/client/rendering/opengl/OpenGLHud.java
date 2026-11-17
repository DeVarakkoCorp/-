package dev.aether.client.rendering.opengl;

import dev.aether.client.rendering.HotbarLayout;
import dev.aether.client.rendering.InventoryLayout;
import dev.aether.client.rendering.KeyBindingEditorLayout;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/** Small render-thread-owned OpenGL overlay for hotbar selection and occupied slots. */
final class OpenGLHud implements AutoCloseable {
    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec2 position;
            layout (location = 1) in vec3 vertexColor;
            out vec3 color;
            void main() {
                gl_Position = vec4(position, 0.0, 1.0);
                color = vertexColor;
            }
            """;
    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in vec3 color;
            out vec4 fragmentColor;
            void main() {
                fragmentColor = vec4(color, 1.0);
            }
            """;

    private final Thread ownerThread = Thread.currentThread();
    private int program;
    private int vertexArray;
    private int vertexBuffer;
    private boolean closed;

    OpenGLHud() {
        int vertex = compile(GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragment = compile(GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        program = glCreateProgram();
        glAttachShader(program, vertex);
        glAttachShader(program, fragment);
        glLinkProgram(program);
        glDeleteShader(vertex);
        glDeleteShader(fragment);
        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            program = 0;
            throw new IllegalStateException("Hotbar shader link failed: " + log);
        }
        vertexArray = glGenVertexArrays();
        vertexBuffer = glGenBuffers();
        glBindVertexArray(vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 5 * Float.BYTES, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 5 * Float.BYTES, 2L * Float.BYTES);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    void draw(int width, int height, int selectedSlot, boolean[] occupied,
              int[] inventoryItemIds, int[] inventoryCounts, boolean inventoryOpen,
              int[] craftingItemIds, int[] craftingCounts, int resultItemId,
              int resultCount, int selectedCraftSlot, String[] editorActions,
              String[] editorBindings, int selectedEditorRow, boolean editorOpen, boolean editorCapturing) {
        checkThread();
        float[] vertices = editorOpen
                ? KeyBindingEditorLayout.vertices(width, height, editorActions, editorBindings,
                        selectedEditorRow, editorCapturing)
                : inventoryOpen
                ? InventoryLayout.vertices(width, height, selectedSlot, inventoryItemIds, inventoryCounts,
                        craftingItemIds, craftingCounts, resultItemId, resultCount, selectedCraftSlot)
                : HotbarLayout.vertices(width, height, selectedSlot, occupied);
        glDisable(GL_DEPTH_TEST);
        glUseProgram(program);
        glBindVertexArray(vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STREAM_DRAW);
        glDrawArrays(GL_TRIANGLES, 0, vertices.length / 5);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
        glUseProgram(0);
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Hotbar shader compilation failed: " + log);
        }
        return shader;
    }

    private void checkThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("Hotbar rendering requires its owning OpenGL thread");
        }
        if (closed) throw new IllegalStateException("Hotbar renderer is closed");
    }

    @Override
    public void close() {
        checkThread();
        glDeleteBuffers(vertexBuffer);
        glDeleteVertexArrays(vertexArray);
        glDeleteProgram(program);
        vertexBuffer = 0;
        vertexArray = 0;
        program = 0;
        closed = true;
    }
}
