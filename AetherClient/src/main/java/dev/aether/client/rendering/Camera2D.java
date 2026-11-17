package dev.aether.client.rendering;

public final class Camera2D {
    private static final float MIN_OFFSET = -1.5f;
    private static final float MAX_OFFSET = 1.5f;
    private static final float MIN_ZOOM = 0.45f;
    private static final float MAX_ZOOM = 2.5f;

    private float offsetX;
    private float offsetY;
    private float zoom = 1.0f;

    public void pan(float deltaX, float deltaY) {
        if (!Float.isFinite(deltaX) || !Float.isFinite(deltaY)) {
            throw new IllegalArgumentException("Camera movement must be finite");
        }
        offsetX = clamp(offsetX + deltaX, MIN_OFFSET, MAX_OFFSET);
        offsetY = clamp(offsetY + deltaY, MIN_OFFSET, MAX_OFFSET);
    }

    public void zoom(float delta) {
        if (!Float.isFinite(delta)) throw new IllegalArgumentException("Zoom change must be finite");
        zoom = clamp(zoom + delta, MIN_ZOOM, MAX_ZOOM);
    }

    public float offsetX() { return offsetX; }
    public float offsetY() { return offsetY; }
    public float zoom() { return zoom; }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
