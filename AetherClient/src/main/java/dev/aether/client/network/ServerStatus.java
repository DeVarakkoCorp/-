package dev.aether.client.network;

import java.util.Objects;

/** Raw status JSON and measured round-trip latency returned by a Java server. */
public record ServerStatus(String json, long latencyMillis) {
    public ServerStatus {
        Objects.requireNonNull(json, "json");
        if (latencyMillis < 0) throw new IllegalArgumentException("latencyMillis must not be negative");
    }
}
