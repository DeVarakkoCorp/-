package dev.aether.client.game;

public final class PlayerState {
    double x;
    double y;
    double z;
    double velocityX;
    double velocityY;
    double velocityZ;
    boolean onGround;

    public PlayerState(double x, double y, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Player position must be finite");
        }
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public double velocityY() { return velocityY; }
    public boolean onGround() { return onGround; }
}
