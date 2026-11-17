package dev.aether.client.network;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolIOTest {
    @Test
    void roundTripsSignedAndUnsignedVarIntBoundaries() throws IOException {
        for (int value : new int[]{0, 1, 127, 128, Integer.MAX_VALUE, Integer.MIN_VALUE, -1}) {
            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            ProtocolIO.writeVarInt(encoded, value);

            ByteArrayInputStream input = new ByteArrayInputStream(encoded.toByteArray());
            assertEquals(value, ProtocolIO.readVarInt(input));
            assertEquals(0, input.available());
        }
    }

    @Test
    void rejectsTruncatedOverlongAndOverflowingVarInts() {
        assertThrows(IOException.class, () -> ProtocolIO.readVarInt(new ByteArrayInputStream(new byte[]{(byte) 0x80})));
        assertThrows(IOException.class, () -> ProtocolIO.readVarInt(
                new ByteArrayInputStream(new byte[]{(byte) 0x80, 0})));
        assertThrows(IOException.class, () -> ProtocolIO.readVarInt(
                new ByteArrayInputStream(new byte[]{(byte) 0x80, (byte) 0x80, (byte) 0x80,
                        (byte) 0x80, (byte) 0x80})));
    }
}
