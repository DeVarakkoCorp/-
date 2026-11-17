package dev.aether.client.game;

public interface GameInputState {
    boolean keyDown(GameKey key);
    boolean mouseDown(MouseButton button);
    boolean consumeKeyPress(GameKey key);
    boolean consumeMousePress(MouseButton button);
}
