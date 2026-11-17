package dev.aether.client.rendering;

import dev.aether.client.game.InputAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyBindingEditorLayoutTest {
    @Test
    void buildsVisibleActionAndBindingRowsForEveryInputAction() {
        String[] actions = java.util.Arrays.stream(InputAction.values())
                .map(action -> action.name().replace('_', ' ')).toArray(String[]::new);
        String[] bindings = java.util.Arrays.stream(InputAction.values()).map(action -> "KEY W").toArray(String[]::new);

        float[] normal = KeyBindingEditorLayout.vertices(1280, 720, actions, bindings, 0, false);
        float[] capturing = KeyBindingEditorLayout.vertices(1280, 720, actions, bindings, 4, true);

        assertTrue(normal.length > actions.length * 6 * 5);
        assertTrue(capturing.length > actions.length * 6 * 5);
        assertNotEquals(normal.length, capturing.length);
        assertEquals(0, normal.length % 5);
    }

    @Test
    void rejectsInvalidSelectionAndViewportsThatCannotFitTheWholeList() {
        String[] actions = {"MOVE"};
        String[] bindings = {"KEY W"};
        assertThrows(IllegalArgumentException.class,
                () -> KeyBindingEditorLayout.vertices(1280, 720, actions, bindings, 1, false));
        assertThrows(IllegalArgumentException.class,
                () -> KeyBindingEditorLayout.vertices(1280, 100, actions, bindings, 0, false));
    }
}
