package dev.aether.client.platform;

import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWWindowSizeCallback;
import dev.aether.client.game.GameInputState;
import dev.aether.client.game.GameKey;
import dev.aether.client.game.MouseButton;

import java.util.HashSet;
import java.util.Set;
import java.util.ArrayDeque;
import java.util.Deque;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class GlfwWindow implements AutoCloseable, GameInputState {
    private final int width;
    private final int height;
    private final String title;
    private long handle;
    private int framebufferWidth;
    private int framebufferHeight;
    private GLFWFramebufferSizeCallback framebufferCallback;
    private GLFWCursorPosCallback cursorCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private GLFWKeyCallback keyCallback;
    private GLFWWindowSizeCallback windowSizeCallback;
    private final Set<Integer> pressedKeys = new HashSet<>();
    private final Set<Integer> keyPressEvents = new HashSet<>();
    private final Deque<Integer> keyPressOrder = new ArrayDeque<>();
    private final Set<Integer> pressedMouseButtons = new HashSet<>();
    private final Set<Integer> mousePressEvents = new HashSet<>();
    private double cursorX;
    private double cursorY;
    private int windowWidth;
    private int windowHeight;
    private boolean escapeClosesWindow = true;

    public GlfwWindow(int width, int height, String title) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Window dimensions must be positive");
        }
        this.width = width;
        this.height = height;
        this.windowWidth = width;
        this.windowHeight = height;
        this.title = title;
    }

    public void create() {
        if (handle != NULL) {
            throw new IllegalStateException("Window already created");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        long created = glfwCreateWindow(width, height, title, NULL, NULL);
        if (created == NULL) {
            throw new IllegalStateException("Failed to create GLFW window");
        }

        handle = created;
        int[] w = new int[1];
        int[] h = new int[1];
        glfwGetFramebufferSize(handle, w, h);
        framebufferWidth = w[0];
        framebufferHeight = h[0];

        framebufferCallback = GLFWFramebufferSizeCallback.create((window, newWidth, newHeight) -> {
            framebufferWidth = newWidth;
            framebufferHeight = newHeight;
        });
        glfwSetFramebufferSizeCallback(handle, framebufferCallback);
        windowSizeCallback = GLFWWindowSizeCallback.create((window, newWidth, newHeight) -> {
            windowWidth = newWidth;
            windowHeight = newHeight;
        });
        glfwSetWindowSizeCallback(handle, windowSizeCallback);
        cursorCallback = GLFWCursorPosCallback.create((window, x, y) -> {
            cursorX = x;
            cursorY = y;
        });
        glfwSetCursorPosCallback(handle, cursorCallback);
        mouseButtonCallback = GLFWMouseButtonCallback.create((window, button, action, mods) -> {
            if (action == GLFW_PRESS) {
                pressedMouseButtons.add(button);
                mousePressEvents.add(button);
            } else if (action == GLFW_RELEASE) {
                pressedMouseButtons.remove(button);
            }
        });
        glfwSetMouseButtonCallback(handle, mouseButtonCallback);

        keyCallback = GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS && escapeClosesWindow) {
                glfwSetWindowShouldClose(window, true);
            }
            if (action == GLFW_PRESS) {
                pressedKeys.add(key);
                keyPressEvents.add(key);
                keyPressOrder.addLast(key);
            }
            if (action == GLFW_RELEASE) pressedKeys.remove(key);
        });
        glfwSetKeyCallback(handle, keyCallback);
    }

    public long handle() {
        ensureCreated();
        return handle;
    }

    public boolean shouldClose() {
        ensureCreated();
        return glfwWindowShouldClose(handle);
    }

    public int framebufferWidth() {
        return framebufferWidth;
    }

    public int framebufferHeight() {
        return framebufferHeight;
    }

    public boolean isKeyDown(int key) {
        return pressedKeys.contains(key);
    }

    public GameKey consumeAnyKeyPress() {
        while (!keyPressOrder.isEmpty()) {
            int key = keyPressOrder.removeFirst();
            if (keyPressEvents.remove(key)) return gameKey(key);
        }
        return null;
    }

    public MouseButton consumeAnyMousePress() {
        if (mousePressEvents.remove(GLFW_MOUSE_BUTTON_LEFT)) return MouseButton.LEFT;
        if (mousePressEvents.remove(GLFW_MOUSE_BUTTON_RIGHT)) return MouseButton.RIGHT;
        if (mousePressEvents.remove(GLFW_MOUSE_BUTTON_MIDDLE)) return MouseButton.MIDDLE;
        return null;
    }

    public void setEscapeClosesWindow(boolean closes) {
        escapeClosesWindow = closes;
    }

    @Override
    public boolean keyDown(GameKey key) {
        return isKeyDown(glfwKey(key));
    }

    @Override
    public boolean mouseDown(MouseButton button) {
        return isMouseButtonDown(glfwMouseButton(button));
    }

    @Override
    public boolean consumeKeyPress(GameKey key) {
        return keyPressEvents.remove(glfwKey(key));
    }

    public boolean consumeMousePress(int button) {
        return mousePressEvents.remove(button);
    }

    @Override
    public boolean consumeMousePress(MouseButton button) {
        return consumeMousePress(glfwMouseButton(button));
    }

    public boolean isMouseButtonDown(int button) {
        return pressedMouseButtons.contains(button);
    }

    public double cursorX() { return cursorX; }
    public double cursorY() { return cursorY; }
    public int windowWidth() { return windowWidth; }
    public int windowHeight() { return windowHeight; }

    public void setTitle(String title) {
        ensureCreated();
        glfwSetWindowTitle(handle, java.util.Objects.requireNonNull(title, "title"));
    }

    private static int glfwKey(GameKey key) {
        return switch (java.util.Objects.requireNonNull(key, "key")) {
            case A -> GLFW_KEY_A; case B -> GLFW_KEY_B; case C -> GLFW_KEY_C; case D -> GLFW_KEY_D;
            case E -> GLFW_KEY_E; case F -> GLFW_KEY_F; case G -> GLFW_KEY_G; case H -> GLFW_KEY_H;
            case I -> GLFW_KEY_I; case J -> GLFW_KEY_J; case K -> GLFW_KEY_K; case L -> GLFW_KEY_L;
            case M -> GLFW_KEY_M; case N -> GLFW_KEY_N; case O -> GLFW_KEY_O; case P -> GLFW_KEY_P;
            case Q -> GLFW_KEY_Q; case R -> GLFW_KEY_R; case S -> GLFW_KEY_S; case T -> GLFW_KEY_T;
            case U -> GLFW_KEY_U; case V -> GLFW_KEY_V; case W -> GLFW_KEY_W; case X -> GLFW_KEY_X;
            case Y -> GLFW_KEY_Y; case Z -> GLFW_KEY_Z;
            case SPACE -> GLFW_KEY_SPACE;
            case ESCAPE -> GLFW_KEY_ESCAPE;
            case TAB -> GLFW_KEY_TAB;
            case ENTER -> GLFW_KEY_ENTER;
            case BACKSPACE -> GLFW_KEY_BACKSPACE;
            case LEFT -> GLFW_KEY_LEFT;
            case RIGHT -> GLFW_KEY_RIGHT;
            case UP -> GLFW_KEY_UP;
            case DOWN -> GLFW_KEY_DOWN;
            case EQUAL -> GLFW_KEY_EQUAL;
            case MINUS -> GLFW_KEY_MINUS;
            case DIGIT_0 -> GLFW_KEY_0;
            case DIGIT_1 -> GLFW_KEY_1;
            case DIGIT_2 -> GLFW_KEY_2;
            case DIGIT_3 -> GLFW_KEY_3;
            case DIGIT_4 -> GLFW_KEY_4;
            case DIGIT_5 -> GLFW_KEY_5;
            case DIGIT_6 -> GLFW_KEY_6;
            case DIGIT_7 -> GLFW_KEY_7;
            case DIGIT_8 -> GLFW_KEY_8;
            case DIGIT_9 -> GLFW_KEY_9;
            case F1 -> GLFW_KEY_F1; case F2 -> GLFW_KEY_F2; case F3 -> GLFW_KEY_F3;
            case F4 -> GLFW_KEY_F4; case F5 -> GLFW_KEY_F5; case F6 -> GLFW_KEY_F6;
            case F7 -> GLFW_KEY_F7; case F8 -> GLFW_KEY_F8; case F9 -> GLFW_KEY_F9;
            case F10 -> GLFW_KEY_F10; case F11 -> GLFW_KEY_F11; case F12 -> GLFW_KEY_F12;
        };
    }

    private static GameKey gameKey(int key) {
        return switch (key) {
            case GLFW_KEY_A -> GameKey.A; case GLFW_KEY_B -> GameKey.B; case GLFW_KEY_C -> GameKey.C;
            case GLFW_KEY_D -> GameKey.D; case GLFW_KEY_E -> GameKey.E; case GLFW_KEY_F -> GameKey.F;
            case GLFW_KEY_G -> GameKey.G; case GLFW_KEY_H -> GameKey.H; case GLFW_KEY_I -> GameKey.I;
            case GLFW_KEY_J -> GameKey.J; case GLFW_KEY_K -> GameKey.K; case GLFW_KEY_L -> GameKey.L;
            case GLFW_KEY_M -> GameKey.M; case GLFW_KEY_N -> GameKey.N; case GLFW_KEY_O -> GameKey.O;
            case GLFW_KEY_P -> GameKey.P; case GLFW_KEY_Q -> GameKey.Q; case GLFW_KEY_R -> GameKey.R;
            case GLFW_KEY_S -> GameKey.S; case GLFW_KEY_T -> GameKey.T; case GLFW_KEY_U -> GameKey.U;
            case GLFW_KEY_V -> GameKey.V; case GLFW_KEY_W -> GameKey.W; case GLFW_KEY_X -> GameKey.X;
            case GLFW_KEY_Y -> GameKey.Y; case GLFW_KEY_Z -> GameKey.Z;
            case GLFW_KEY_SPACE -> GameKey.SPACE; case GLFW_KEY_ESCAPE -> GameKey.ESCAPE;
            case GLFW_KEY_TAB -> GameKey.TAB;
            case GLFW_KEY_ENTER -> GameKey.ENTER; case GLFW_KEY_BACKSPACE -> GameKey.BACKSPACE;
            case GLFW_KEY_LEFT -> GameKey.LEFT; case GLFW_KEY_RIGHT -> GameKey.RIGHT;
            case GLFW_KEY_UP -> GameKey.UP; case GLFW_KEY_DOWN -> GameKey.DOWN;
            case GLFW_KEY_EQUAL -> GameKey.EQUAL; case GLFW_KEY_MINUS -> GameKey.MINUS;
            case GLFW_KEY_0 -> GameKey.DIGIT_0; case GLFW_KEY_1 -> GameKey.DIGIT_1;
            case GLFW_KEY_2 -> GameKey.DIGIT_2; case GLFW_KEY_3 -> GameKey.DIGIT_3;
            case GLFW_KEY_4 -> GameKey.DIGIT_4; case GLFW_KEY_5 -> GameKey.DIGIT_5;
            case GLFW_KEY_6 -> GameKey.DIGIT_6; case GLFW_KEY_7 -> GameKey.DIGIT_7;
            case GLFW_KEY_8 -> GameKey.DIGIT_8; case GLFW_KEY_9 -> GameKey.DIGIT_9;
            case GLFW_KEY_F1 -> GameKey.F1; case GLFW_KEY_F2 -> GameKey.F2; case GLFW_KEY_F3 -> GameKey.F3;
            case GLFW_KEY_F4 -> GameKey.F4; case GLFW_KEY_F5 -> GameKey.F5; case GLFW_KEY_F6 -> GameKey.F6;
            case GLFW_KEY_F7 -> GameKey.F7; case GLFW_KEY_F8 -> GameKey.F8; case GLFW_KEY_F9 -> GameKey.F9;
            case GLFW_KEY_F10 -> GameKey.F10; case GLFW_KEY_F11 -> GameKey.F11; case GLFW_KEY_F12 -> GameKey.F12;
            default -> null;
        };
    }

    private static int glfwMouseButton(MouseButton button) {
        return switch (java.util.Objects.requireNonNull(button, "button")) {
            case LEFT -> GLFW_MOUSE_BUTTON_LEFT;
            case RIGHT -> GLFW_MOUSE_BUTTON_RIGHT;
            case MIDDLE -> GLFW_MOUSE_BUTTON_MIDDLE;
        };
    }

    private void ensureCreated() {
        if (handle == NULL) {
            throw new IllegalStateException("Window is not created");
        }
    }

    @Override
    public void close() {
        if (handle == NULL) {
            return;
        }
        glfwSetFramebufferSizeCallback(handle, null);
        glfwSetKeyCallback(handle, null);
        if (framebufferCallback != null) {
            framebufferCallback.free();
            framebufferCallback = null;
        }
        if (cursorCallback != null) {
            cursorCallback.free();
            cursorCallback = null;
        }
        if (mouseButtonCallback != null) {
            mouseButtonCallback.free();
            mouseButtonCallback = null;
        }
        if (keyCallback != null) {
            keyCallback.free();
            keyCallback = null;
        }
        if (windowSizeCallback != null) {
            windowSizeCallback.free();
            windowSizeCallback = null;
        }
        glfwDestroyWindow(handle);
        handle = NULL;
    }
}
