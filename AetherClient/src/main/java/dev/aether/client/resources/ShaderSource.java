package dev.aether.client.resources;

import java.util.List;

public record ShaderSource(String path, String source, List<String> dependencies) {
    public ShaderSource {
        dependencies = List.copyOf(dependencies);
    }
}
