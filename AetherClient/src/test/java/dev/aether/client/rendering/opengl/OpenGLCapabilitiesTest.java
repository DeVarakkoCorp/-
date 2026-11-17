package dev.aether.client.rendering.opengl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OpenGLCapabilitiesTest {
    @Test
    void acceptsValidDeviceSnapshot() {
        assertDoesNotThrow(() -> new OpenGLCapabilities(
                "Intel", "Intel UHD", "3.3.0", 3, 3, 16384, 8, 16));
    }

    @Test
    void rejectsMissingIdentityAndInvalidVersion() {
        assertThrows(IllegalArgumentException.class, () -> new OpenGLCapabilities(
                " ", "GPU", "3.3", 3, 3, 4096, 8, 4));
        assertThrows(IllegalArgumentException.class, () -> new OpenGLCapabilities(
                "Vendor", "GPU", "3.3", 0, 3, 4096, 8, 4));
    }

    @Test
    void rejectsInvalidDeviceLimits() {
        assertThrows(IllegalArgumentException.class, () -> new OpenGLCapabilities(
                "Vendor", "GPU", "3.3", 3, 3, 0, 8, 4));
        assertThrows(IllegalArgumentException.class, () -> new OpenGLCapabilities(
                "Vendor", "GPU", "3.3", 3, 3, 4096, 0, 4));
        assertThrows(IllegalArgumentException.class, () -> new OpenGLCapabilities(
                "Vendor", "GPU", "3.3", 3, 3, 4096, 8, -1));
    }
}
