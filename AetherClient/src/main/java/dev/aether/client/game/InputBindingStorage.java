package dev.aether.client.game;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;
import java.util.Locale;

/** Reads and writes the human-editable keybindings.properties file atomically. */
public final class InputBindingStorage {
    private static final String VERSION = "1";

    private InputBindingStorage() {}

    public static InputBindings load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (Files.notExists(path)) return InputBindings.defaults();

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            properties.load(reader);
        }
        if (!VERSION.equals(properties.getProperty("version"))) {
            throw new IOException("Unsupported keybinding file version");
        }

        InputBindings defaults = InputBindings.defaults();
        InputBindings loaded = new InputBindings();
        for (InputAction action : InputAction.values()) {
            String value = properties.getProperty(action.name());
            if (value == null) {
                GameKey defaultKey = defaults.keyFor(action);
                MouseButton defaultMouse = defaults.mouseButtonFor(action);
                if (defaultKey != null) loaded.bindKey(action, defaultKey);
                else if (defaultMouse != null) loaded.bindMouse(action, defaultMouse);
                else throw new IOException("No default binding for " + action);
                continue;
            }
            try {
                if (value.startsWith("key:")) {
                    loaded.bindKey(action, GameKey.valueOf(value.substring(4).trim().toUpperCase(Locale.ROOT)));
                } else if (value.startsWith("mouse:")) {
                    loaded.bindMouse(action, MouseButton.valueOf(value.substring(6).trim().toUpperCase(Locale.ROOT)));
                } else {
                    throw new IllegalArgumentException("Expected key:NAME or mouse:NAME");
                }
            } catch (IllegalArgumentException invalid) {
                throw new IOException("Invalid keybinding for " + action + ": " + value, invalid);
            }
        }
        return loaded;
    }

    public static void save(Path path, InputBindings bindings) throws IOException {
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(bindings, "bindings");
        Properties properties = new Properties();
        properties.setProperty("version", VERSION);
        for (InputAction action : InputAction.values()) {
            GameKey key = bindings.keyFor(action);
            MouseButton mouse = bindings.mouseButtonFor(action);
            if (key != null) properties.setProperty(action.name(), "key:" + key.name());
            else if (mouse != null) properties.setProperty(action.name(), "mouse:" + mouse.name());
            else throw new IOException("Action has no binding: " + action);
        }

        Path absolute = path.toAbsolutePath().normalize();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                properties.store(writer, "Aether Client keybindings. Values use key:NAME or mouse:NAME.");
            }
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
