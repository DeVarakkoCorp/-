package dev.aether.client.game;

public record PlayerInput(float strafe, float forward, boolean jump) {
    public PlayerInput {
        if (!Float.isFinite(strafe) || !Float.isFinite(forward)) {
            throw new IllegalArgumentException("Movement input must be finite");
        }
    }
}
