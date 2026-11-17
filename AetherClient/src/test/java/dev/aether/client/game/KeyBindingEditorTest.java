package dev.aether.client.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyBindingEditorTest {
    @Test
    void wrapsSelectionAndCapturesUnboundKeyboardAndMouseInputs() {
        KeyBindingEditor editor = new KeyBindingEditor();
        InputBindings bindings = InputBindings.defaults();

        editor.moveSelection(-1);
        assertEquals(InputAction.TOGGLE_INVENTORY, editor.selectedAction());
        editor.moveSelection(1);
        assertEquals(InputAction.MOVE_FORWARD, editor.selectedAction());
        editor.beginCapture();
        assertTrue(editor.capture(GameKey.F, null, bindings));
        assertEquals(GameKey.F, bindings.keyFor(InputAction.MOVE_FORWARD));

        editor.moveSelection(11); // reaches MINE in declaration order
        assertEquals(InputAction.MINE, editor.selectedAction());
        editor.beginCapture();
        assertTrue(editor.capture(null, MouseButton.MIDDLE, bindings));
        assertEquals(MouseButton.MIDDLE, bindings.mouseButtonFor(InputAction.MINE));
    }

    @Test
    void conflictingInputLeavesCaptureActiveAndEscapeCancels() {
        KeyBindingEditor editor = new KeyBindingEditor();
        InputBindings bindings = InputBindings.defaults();
        editor.beginCapture();
        assertFalse(editor.capture(GameKey.W, null, bindings));
        assertTrue(editor.capturing());
        assertTrue(editor.message().contains("already bound"));
        assertFalse(editor.capture(GameKey.ESCAPE, null, bindings));
        assertFalse(editor.capturing());
    }
}
