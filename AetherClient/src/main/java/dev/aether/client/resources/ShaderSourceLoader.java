package dev.aether.client.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShaderSourceLoader {
    private static final Pattern INCLUDE = Pattern.compile(
            "(?m)^\\s*#include\\s+[\"<]([^\">]+)[\">]\\s*$"
    );
    private static final Pattern VERSION = Pattern.compile("(?m)^\\s*#version\\b.*$");
    private static final int MAX_DEPTH = 32;

    private final ClassLoader classLoader;
    private final String root;

    public ShaderSourceLoader(ClassLoader classLoader, String root) {
        this.classLoader = classLoader;
        this.root = normalizeRoot(root);
    }

    public ShaderSource load(String relativePath) throws IOException {
        String path = resolveRoot(relativePath);
        LinkedHashSet<String> dependencies = new LinkedHashSet<>();
        String source = expand(path, new ArrayList<>(), dependencies, 0, true);
        if (!VERSION.matcher(source).find()) {
            throw new IOException("Root shader must declare #version: " + path);
        }
        return new ShaderSource(path, source, List.copyOf(dependencies));
    }

    private String expand(
            String path,
            List<String> stack,
            Set<String> dependencies,
            int depth,
            boolean rootFile
    ) throws IOException {
        if (depth > MAX_DEPTH) throw new IOException("Shader include depth exceeds " + MAX_DEPTH);
        if (stack.contains(path)) {
            List<String> cycle = new ArrayList<>(stack);
            cycle.add(path);
            throw new IOException("Circular shader include: " + String.join(" -> ", cycle));
        }

        String text = read(path);
        if (!rootFile && VERSION.matcher(text).find()) {
            throw new IOException("Included shader must not declare #version: " + path);
        }

        stack.add(path);
        dependencies.add(path);
        StringBuilder output = new StringBuilder();
        Matcher matcher = INCLUDE.matcher(text);
        int cursor = 0;
        while (matcher.find()) {
            output.append(text, cursor, matcher.start());
            String child = resolveRelative(path, matcher.group(1));
            output.append("\n#line 1\n");
            output.append(expand(child, stack, dependencies, depth + 1, false));
            output.append("\n#line ").append(countLines(text, matcher.end()) + 1).append("\n");
            cursor = matcher.end();
        }
        output.append(text.substring(cursor));
        stack.remove(stack.size() - 1);
        return output.toString();
    }

    private String read(String path) throws IOException {
        try (InputStream stream = classLoader.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Shader resource not found: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String resolveRoot(String path) throws IOException {
        String normalized = normalizeRelative(path);
        String full = root + "/" + normalized;
        if (!full.startsWith(root + "/")) throw new IOException("Shader path escapes root");
        return full;
    }

    private String resolveRelative(String parent, String include) throws IOException {
        if (include.startsWith("/") || include.contains(":")) {
            throw new IOException("Absolute shader include is forbidden: " + include);
        }
        String parentDir = parent.substring(0, parent.lastIndexOf('/'));
        String combined = parentDir + "/" + include.replace('\\', '/');
        String normalized = normalizeSegments(combined);
        if (!normalized.startsWith(root + "/")) {
            throw new IOException("Shader include escapes shader root: " + include);
        }
        return normalized;
    }

    private String normalizeRoot(String value) {
        String normalized = value.replace('\\', '/');
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        if (normalized.isBlank() || normalized.contains("..") || normalized.contains(":")) {
            throw new IllegalArgumentException("Invalid shader root");
        }
        return normalized;
    }

    private String normalizeRelative(String value) throws IOException {
        if (value == null || value.isBlank() || value.startsWith("/") || value.contains(":")) {
            throw new IOException("Invalid shader path: " + value);
        }
        String normalized = normalizeSegments(value.replace('\\', '/'));
        if (normalized.equals("..") || normalized.startsWith("../")) {
            throw new IOException("Shader path escapes root: " + value);
        }
        return normalized;
    }

    private String normalizeSegments(String path) throws IOException {
        List<String> parts = new ArrayList<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (parts.isEmpty()) throw new IOException("Path traversal rejected: " + path);
                parts.remove(parts.size() - 1);
            } else {
                parts.add(part);
            }
        }
        return String.join("/", parts);
    }

    private static int countLines(String text, int end) {
        int count = 0;
        for (int i = 0; i < end; i++) {
            if (text.charAt(i) == '\n') count++;
        }
        return count;
    }
}
