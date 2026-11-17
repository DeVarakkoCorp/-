package dev.aether.client.game;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** User-remappable mapping from game actions to keyboard or mouse inputs. */
public final class InputBindings {
    private final Map<InputAction, GameKey> keys = new EnumMap<>(InputAction.class);
    private final Map<InputAction, MouseButton> mouseButtons = new EnumMap<>(InputAction.class);

    public static InputBindings defaults() {
        InputBindings bindings = new InputBindings();
        bindings.bindKey(InputAction.MOVE_FORWARD, GameKey.W);
        bindings.bindKey(InputAction.MOVE_BACKWARD, GameKey.S);
        bindings.bindKey(InputAction.MOVE_LEFT, GameKey.A);
        bindings.bindKey(InputAction.MOVE_RIGHT, GameKey.D);
        bindings.bindKey(InputAction.JUMP, GameKey.SPACE);
        bindings.bindKey(InputAction.CAMERA_LEFT, GameKey.LEFT);
        bindings.bindKey(InputAction.CAMERA_RIGHT, GameKey.RIGHT);
        bindings.bindKey(InputAction.CAMERA_UP, GameKey.UP);
        bindings.bindKey(InputAction.CAMERA_DOWN, GameKey.DOWN);
        bindings.bindKey(InputAction.ZOOM_IN, GameKey.EQUAL);
        bindings.bindKey(InputAction.ZOOM_OUT, GameKey.MINUS);
        bindings.bindMouse(InputAction.MINE, MouseButton.LEFT);
        bindings.bindMouse(InputAction.PLACE, MouseButton.RIGHT);
        bindings.bindKey(InputAction.HOTBAR_1, GameKey.DIGIT_1);
        bindings.bindKey(InputAction.HOTBAR_2, GameKey.DIGIT_2);
        bindings.bindKey(InputAction.HOTBAR_3, GameKey.DIGIT_3);
        bindings.bindKey(InputAction.HOTBAR_4, GameKey.DIGIT_4);
        bindings.bindKey(InputAction.HOTBAR_5, GameKey.DIGIT_5);
        bindings.bindKey(InputAction.HOTBAR_6, GameKey.DIGIT_6);
        bindings.bindKey(InputAction.HOTBAR_7, GameKey.DIGIT_7);
        bindings.bindKey(InputAction.HOTBAR_8, GameKey.DIGIT_8);
        bindings.bindKey(InputAction.HOTBAR_9, GameKey.DIGIT_9);
        bindings.bindKey(InputAction.TOGGLE_INVENTORY, GameKey.E);
        return bindings;
    }

    public void bindKey(InputAction action, GameKey key) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(key, "key");
        ensureAvailable(action, key);
        keys.put(action, key);
        mouseButtons.remove(action);
    }

    public void bindMouse(InputAction action, MouseButton button) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(button, "button");
        ensureAvailable(action, button);
        mouseButtons.put(action, button);
        keys.remove(action);
    }

    public boolean isDown(InputAction action, GameInputState input) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(input, "input");
        GameKey key = keys.get(action);
        if (key != null) return input.keyDown(key);
        MouseButton button = mouseButtons.get(action);
        return button != null && input.mouseDown(button);
    }

    public boolean consumePress(InputAction action, GameInputState input) {
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(input, "input");
        GameKey key = keys.get(action);
        if (key != null) return input.consumeKeyPress(key);
        MouseButton button = mouseButtons.get(action);
        return button != null && input.consumeMousePress(button);
    }

    public GameKey keyFor(InputAction action) { return keys.get(Objects.requireNonNull(action, "action")); }
    public MouseButton mouseButtonFor(InputAction action) {
        return mouseButtons.get(Objects.requireNonNull(action, "action"));
    }

    private void ensureAvailable(InputAction action, GameKey key) {
        Set<InputAction> conflicts = new HashSet<>();
        keys.forEach((boundAction, boundKey) -> {
            if (boundKey == key && boundAction != action) conflicts.add(boundAction);
        });
        if (!conflicts.isEmpty()) throw new IllegalArgumentException("Key is already bound to " + conflicts);
    }

    private void ensureAvailable(InputAction action, MouseButton button) {
        Set<InputAction> conflicts = new HashSet<>();
        mouseButtons.forEach((boundAction, boundButton) -> {
            if (boundButton == button && boundAction != action) conflicts.add(boundAction);
        });
        if (!conflicts.isEmpty()) throw new IllegalArgumentException("Mouse button is already bound to " + conflicts);
    }
}
