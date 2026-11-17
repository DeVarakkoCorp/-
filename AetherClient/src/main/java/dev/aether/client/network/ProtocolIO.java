package dev.aether.client.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/** Bounded framing and primitive encodings used by the uncompressed status protocol. */
final class ProtocolIO {
    static final int MAX_FRAME_BYTES = 2 * 1024 * 1024;
    static final int MAX_STRING_BYTES = 32_767;

    private ProtocolIO() {}

    static void writeVarInt(OutputStream output, int value) throws IOException {
        while ((value & 0xFFFFFF80) != 0) {
            output.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        output.write(value & 0x7F);
    }

    static int readVarInt(InputStream input) throws IOException {
        int value = 0;
        for (int index = 0; index < 5; index++) {
            int current = input.read();
            if (current < 0) throw new EOFException("Unexpected end of VarInt");
            if (index == 4 && (current & 0xF0) != 0) throw new IOException("VarInt exceeds 32 bits");
            value |= (current & 0x7F) << (index * 7);
            if ((current & 0x80) == 0) {
                if (index > 0 && current == 0) throw new IOException("Non-canonical VarInt encoding");
                return value;
            }
        }
        throw new IOException("VarInt exceeds five bytes");
    }

    static void writeString(OutputStream output, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > MAX_STRING_BYTES) throw new IOException("Protocol string exceeds byte limit");
        writeVarInt(output, bytes.length);
        output.write(bytes);
    }

    static String readString(InputStream input) throws IOException {
        int length = readVarInt(input);
        if (length < 0 || length > MAX_STRING_BYTES) throw new IOException("Invalid protocol string length");
        byte[] bytes = input.readNBytes(length);
        if (bytes.length != length) throw new EOFException("Truncated protocol string");
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException invalidUtf8) {
            throw new IOException("Invalid UTF-8 protocol string", invalidUtf8);
        }
    }

    static void writeFrame(OutputStream output, byte[] packet) throws IOException {
        if (packet.length == 0 || packet.length > MAX_FRAME_BYTES) {
            throw new IOException("Invalid outgoing packet length: " + packet.length);
        }
        writeVarInt(output, packet.length);
        output.write(packet);
        output.flush();
    }

    static byte[] readFrame(InputStream input) throws IOException {
        int length = readVarInt(input);
        if (length <= 0 || length > MAX_FRAME_BYTES) throw new IOException("Invalid packet length: " + length);
        byte[] packet = input.readNBytes(length);
        if (packet.length != length) throw new EOFException("Truncated packet frame");
        return packet;
    }

    static byte[] packet(PacketWriter writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            writer.write(output);
        }
        if (bytes.size() == 0 || bytes.size() > MAX_FRAME_BYTES) {
            throw new IOException("Invalid packet payload length: " + bytes.size());
        }
        return bytes.toByteArray();
    }

    @FunctionalInterface
    interface PacketWriter {
        void write(DataOutputStream output) throws IOException;
    }
}
