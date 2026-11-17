package dev.aether.client.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;

/** Queries the uncompressed Java Edition server-list status protocol (1.20.6 / protocol 766). */
public final class MinecraftStatusClient {
    public static final int PROTOCOL_VERSION = 766;
    private static final int HANDSHAKE_PACKET_ID = 0x00;
    private static final int STATUS_REQUEST_PACKET_ID = 0x00;
    private static final int STATUS_RESPONSE_PACKET_ID = 0x00;
    private static final int PING_PACKET_ID = 0x01;
    private static final int PONG_PACKET_ID = 0x01;
    private static final int STATUS_NEXT_STATE = 1;

    public ServerStatus query(String host, int port, Duration timeout) throws IOException {
        Objects.requireNonNull(host, "host");
        Objects.requireNonNull(timeout, "timeout");
        if (host.isBlank() || host.length() > 255) throw new IllegalArgumentException("Invalid server host");
        if (port < 1 || port > 65_535) throw new IllegalArgumentException("Port must be 1..65535");
        long timeoutMillis = timeout.toMillis();
        if (timeoutMillis < 1 || timeoutMillis > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Timeout must be between 1 ms and Integer.MAX_VALUE ms");
        }
        if (host.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 1020) {
            throw new IllegalArgumentException("UTF-8 server host is too long");
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) timeoutMillis);
            socket.setSoTimeout((int) timeoutMillis);
            var input = socket.getInputStream();
            var output = socket.getOutputStream();

            ProtocolIO.writeFrame(output, ProtocolIO.packet(packet -> {
                ProtocolIO.writeVarInt(packet, HANDSHAKE_PACKET_ID);
                ProtocolIO.writeVarInt(packet, PROTOCOL_VERSION);
                ProtocolIO.writeString(packet, host);
                packet.writeShort(port);
                ProtocolIO.writeVarInt(packet, STATUS_NEXT_STATE);
            }));
            ProtocolIO.writeFrame(output, ProtocolIO.packet(packet ->
                    ProtocolIO.writeVarInt(packet, STATUS_REQUEST_PACKET_ID)));

            byte[] responseFrame = ProtocolIO.readFrame(input);
            ByteArrayInputStream responseBytes = new ByteArrayInputStream(responseFrame);
            int responseId = ProtocolIO.readVarInt(responseBytes);
            if (responseId != STATUS_RESPONSE_PACKET_ID) {
                throw new IOException("Expected status response packet 0, got " + responseId);
            }
            String statusJson = ProtocolIO.readString(responseBytes);
            if (responseBytes.available() != 0) throw new IOException("Trailing bytes in status response");

            long pingPayload = System.nanoTime();
            long startNanos = System.nanoTime();
            ProtocolIO.writeFrame(output, ProtocolIO.packet(packet -> {
                ProtocolIO.writeVarInt(packet, PING_PACKET_ID);
                packet.writeLong(pingPayload);
            }));
            byte[] pongFrame = ProtocolIO.readFrame(input);
            try (DataInputStream pong = new DataInputStream(new ByteArrayInputStream(pongFrame))) {
                int pongId = ProtocolIO.readVarInt(pong);
                if (pongId != PONG_PACKET_ID || pong.readLong() != pingPayload || pong.available() != 0) {
                    throw new IOException("Invalid status pong packet");
                }
            }
            long latencyMillis = Math.max(0, (System.nanoTime() - startNanos) / 1_000_000);
            return new ServerStatus(statusJson, latencyMillis);
        }
    }
}
