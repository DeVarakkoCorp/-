package dev.aether.client.game;

import java.util.Objects;

/** Keyboard navigation and conflict handling for the in-game keybinding editor. */
public final class KeyBindingEditor {
    private int selectedAction;
    private boolean capturing;
    private String message = "";

    public int selectedIndex() { return selectedAction; }
    public InputAction selectedAction() { return InputAction.values()[selectedAction]; }
    public boolean capturing() { return capturing; }
    public String message() { return message; }

    public void moveSelection(int offset) {
        selectedAction = Math.floorMod(selectedAction + offset, InputAction.values().length);
        capturing = false;
        message = "";
    }

    public void beginCapture() {
        capturing = true;
        message = "Press a key or mouse button";
    }

    public void reportSaveFailure(String reason) {
        capturing = false;
        message = "Could not save binding: " + Objects.requireNonNull(reason, "reason");
    }

    /** Returns true when the new binding was accepted. Escape cancels capture. */
    public boolean capture(GameKey key, MouseButton mouseButton, InputBindings bindings) {
        if (!capturing) return false;
        Objects.requireNonNull(bindings, "bindings");
        if (key == GameKey.ESCAPE) {
            capturing = false;
            message = "Binding cancelled";
            return false;
        }
        if (key == GameKey.F1) {
            message = "F1 is reserved for opening this editor";
            return false;
        }
        if (key == null && mouseButton == null) return false;
        try {
            if (key != null) bindings.bindKey(selectedAction(), key);
            else bindings.bindMouse(selectedAction(), mouseButton);
            capturing = false;
            message = "Binding saved";
            return true;
        } catch (IllegalArgumentException conflict) {
            message = conflict.getMessage();
            return false;
        }
    }
}
