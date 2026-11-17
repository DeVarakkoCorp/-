package dev.aether.client.game;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

class InputBindingsTest {
    @Test
    void defaultBindingsCoverMovementCameraAndMouseActions() {
        InputBindings bindings = InputBindings.defaults();
        FakeInput input = new FakeInput();
        input.keys.add(GameKey.W);
        input.buttons.add(MouseButton.LEFT);

        assertTrue(bindings.isDown(InputAction.MOVE_FORWARD, input));
        assertTrue(bindings.isDown(InputAction.MINE, input));
        assertEquals(GameKey.SPACE, bindings.keyFor(InputAction.JUMP));
        assertEquals(GameKey.E, bindings.keyFor(InputAction.TOGGLE_INVENTORY));
        assertEquals(MouseButton.RIGHT, bindings.mouseButtonFor(InputAction.PLACE));
    }

    @Test
    void rebindingAnActionMovesItToTheNewInput() {
        InputBindings bindings = InputBindings.defaults();
        bindings.bindKey(InputAction.MOVE_FORWARD, GameKey.Q);
        FakeInput input = new FakeInput();
        input.keys.add(GameKey.Q);

        assertTrue(bindings.isDown(InputAction.MOVE_FORWARD, input));
        assertEquals(GameKey.Q, bindings.keyFor(InputAction.MOVE_FORWARD));
    }

    @Test
    void rejectsDuplicateBindingsAndConsumesPressEdgesOnce() {
        InputBindings bindings = InputBindings.defaults();
        assertThrows(IllegalArgumentException.class,
                () -> bindings.bindKey(InputAction.MOVE_FORWARD, GameKey.A));
        assertThrows(IllegalArgumentException.class,
                () -> bindings.bindMouse(InputAction.PLACE, MouseButton.LEFT));

        FakeInput input = new FakeInput();
        input.mousePresses.add(MouseButton.LEFT);
        assertTrue(bindings.consumePress(InputAction.MINE, input));
        assertFalse(bindings.consumePress(InputAction.MINE, input));
    }

    private static final class FakeInput implements GameInputState {
        private final EnumSet<GameKey> keys = EnumSet.noneOf(GameKey.class);
        private final EnumSet<MouseButton> buttons = EnumSet.noneOf(MouseButton.class);
        private final EnumSet<MouseButton> mousePresses = EnumSet.noneOf(MouseButton.class);

        @Override public boolean keyDown(GameKey key) { return keys.contains(key); }
        @Override public boolean mouseDown(MouseButton button) { return buttons.contains(button); }
        @Override public boolean consumeKeyPress(GameKey key) { return false; }
        @Override public boolean consumeMousePress(MouseButton button) { return mousePresses.remove(button); }
    }
}
