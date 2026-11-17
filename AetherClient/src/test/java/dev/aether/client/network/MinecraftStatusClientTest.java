package dev.aether.client.network;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class MinecraftStatusClientTest {
    @Test
    void performsVersion766StatusHandshakeRequestAndPing() throws Exception {
        String json = "{\"version\":{\"name\":\"1.20.6\",\"protocol\":766}}";
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            var worker = Executors.newSingleThreadExecutor();
            try {
                Future<?> server = worker.submit(() -> serveOneStatusRequest(listener, json));
                ServerStatus result = new MinecraftStatusClient().query(
                        "127.0.0.1", listener.getLocalPort(), Duration.ofSeconds(3));

                assertEquals(json, result.json());
                assertTrue(result.latencyMillis() >= 0);
                server.get();
            } finally {
                worker.shutdownNow();
            }
        }
    }

    @Test
    void rejectsInvalidHostPortAndTimeoutBeforeConnecting() {
        MinecraftStatusClient client = new MinecraftStatusClient();
        assertThrows(IllegalArgumentException.class,
                () -> client.query(" ", 25565, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> client.query("localhost", 0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class,
                () -> client.query("localhost", 25565, Duration.ZERO));
    }

    private static void serveOneStatusRequest(ServerSocket listener, String json) {
        try (Socket client = listener.accept()) {
            var input = client.getInputStream();
            var output = client.getOutputStream();

            ByteArrayInputStream handshake = new ByteArrayInputStream(ProtocolIO.readFrame(input));
            assertEquals(0, ProtocolIO.readVarInt(handshake));
            assertEquals(766, ProtocolIO.readVarInt(handshake));
            assertEquals("127.0.0.1", ProtocolIO.readString(handshake));
            assertEquals(listener.getLocalPort(), (handshake.read() << 8) | handshake.read());
            assertEquals(1, ProtocolIO.readVarInt(handshake));
            assertEquals(0, handshake.available());

            ByteArrayInputStream request = new ByteArrayInputStream(ProtocolIO.readFrame(input));
            assertEquals(0, ProtocolIO.readVarInt(request));
            assertEquals(0, request.available());
            ProtocolIO.writeFrame(output, ProtocolIO.packet(packet -> {
                ProtocolIO.writeVarInt(packet, 0);
                ProtocolIO.writeString(packet, json);
            }));

            try (DataInputStream ping = new DataInputStream(new ByteArrayInputStream(ProtocolIO.readFrame(input)))) {
                assertEquals(1, ProtocolIO.readVarInt(ping));
                long nonce = ping.readLong();
                assertEquals(0, ping.available());
                ProtocolIO.writeFrame(output, ProtocolIO.packet(packet -> {
                    ProtocolIO.writeVarInt(packet, 1);
                    packet.writeLong(nonce);
                }));
            }
        } catch (IOException failure) {
            throw new java.io.UncheckedIOException(failure);
        }
    }
}
