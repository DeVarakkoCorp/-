package dev.aether.client.rendering;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RenderTargetDimensionsTest {
    @Test
    void rejectsZeroAndNegativeDimensions() {
        assertThrows(IllegalArgumentException.class, () -> RenderTarget.validateDimensions(0, 10));
        assertThrows(IllegalArgumentException.class, () -> RenderTarget.validateDimensions(10, 0));
        assertThrows(IllegalArgumentException.class, () -> RenderTarget.validateDimensions(-1, 10));
    }

    @Test
    void acceptsPositiveDimensions() {
        assertDoesNotThrow(() -> RenderTarget.validateDimensions(2560, 1440));
    }
}
