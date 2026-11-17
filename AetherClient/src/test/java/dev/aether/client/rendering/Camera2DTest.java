package dev.aether.client.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Camera2DTest {
    @Test
    void pansAndClampsViewOffset() {
        Camera2D camera = new Camera2D();
        camera.pan(0.25f, -0.5f);
        assertEquals(0.25f, camera.offsetX());
        assertEquals(-0.5f, camera.offsetY());
        camera.pan(10.0f, -10.0f);
        assertEquals(1.5f, camera.offsetX());
        assertEquals(-1.5f, camera.offsetY());
    }

    @Test
    void zoomsWithinSafeRange() {
        Camera2D camera = new Camera2D();
        camera.zoom(0.5f);
        assertEquals(1.5f, camera.zoom());
        camera.zoom(100.0f);
        assertEquals(2.5f, camera.zoom());
        camera.zoom(-100.0f);
        assertEquals(0.45f, camera.zoom());
    }

    @Test
    void rejectsNonFiniteInputs() {
        Camera2D camera = new Camera2D();
        assertThrows(IllegalArgumentException.class, () -> camera.pan(Float.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> camera.zoom(Float.POSITIVE_INFINITY));
    }
}
