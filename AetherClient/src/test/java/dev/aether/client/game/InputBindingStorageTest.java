package dev.aether.client.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class InputBindingStorageTest {
    @Test
    void loadsDefaultsWhenSettingsFileIsMissing(@TempDir Path directory) throws IOException {
        assertEquals(GameKey.E,
                InputBindingStorage.load(directory.resolve("keybindings.properties"))
                        .keyFor(InputAction.TOGGLE_INVENTORY));
    }

    @Test
    void roundTripsHumanReadableKeyboardAndMouseBindings(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("keybindings.properties");
        InputBindings bindings = InputBindings.defaults();
        bindings.bindKey(InputAction.TOGGLE_INVENTORY, GameKey.Q);
        bindings.bindMouse(InputAction.MINE, MouseButton.MIDDLE);

        InputBindingStorage.save(path, bindings);
        InputBindings loaded = InputBindingStorage.load(path);

        assertEquals(GameKey.Q, loaded.keyFor(InputAction.TOGGLE_INVENTORY));
        assertEquals(MouseButton.MIDDLE, loaded.mouseButtonFor(InputAction.MINE));
        String text = Files.readString(path);
        assertTrue(text.contains("TOGGLE_INVENTORY"));
        assertTrue(text.contains("key"));
    }

    @Test
    void rejectsInvalidAndConflictingBindings(@TempDir Path directory) throws IOException {
        Path path = directory.resolve("keybindings.properties");
        Files.writeString(path, "version=1\nMOVE_FORWARD=key:W\nMOVE_BACKWARD=key:W\n");
        assertThrows(IOException.class, () -> InputBindingStorage.load(path));

        Files.writeString(path, "version=1\nMOVE_FORWARD=key:NOT_A_KEY\n");
        assertThrows(IOException.class, () -> InputBindingStorage.load(path));
    }
}
