package dev.aether.client.resources;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ShaderSourceLoaderTest {
    @Test
    void expandsNestedIncludesAndTracksDependencies() throws Exception {
        Path temp = Files.createTempDirectory("aether-shaders");
        Path root = temp.resolve("assets/aether/shaders");
        Files.createDirectories(root.resolve("common"));
        Files.writeString(root.resolve("main.vert"),
                "#version 330 core\n#include \"common/a.glsl\"\nvoid main() {}");
        Files.writeString(root.resolve("common/a.glsl"),
                "#include \"b.glsl\"\nfloat a;");
        Files.writeString(root.resolve("common/b.glsl"), "float b;");

        try (URLClassLoader loader = new URLClassLoader(new URL[]{temp.toUri().toURL()}, null)) {
            ShaderSource source = new ShaderSourceLoader(loader, "assets/aether/shaders").load("main.vert");
            assertTrue(source.source().contains("float a;"));
            assertTrue(source.source().contains("float b;"));
            assertEquals(3, source.dependencies().size());
        }
    }

    @Test
    void rejectsIncludeCycle() throws Exception {
        Path temp = Files.createTempDirectory("aether-cycle");
        Path root = temp.resolve("shaders");
        Files.createDirectories(root);
        Files.writeString(root.resolve("main.vert"), "#version 330 core\n#include \"a.glsl\"");
        Files.writeString(root.resolve("a.glsl"), "#include \"main.vert\"");

        try (URLClassLoader loader = new URLClassLoader(new URL[]{temp.toUri().toURL()}, null)) {
            assertThrows(IOException.class,
                    () -> new ShaderSourceLoader(loader, "shaders").load("main.vert"));
        }
    }

    @Test
    void rejectsTraversal() throws Exception {
        Path temp = Files.createTempDirectory("aether-traversal");
        Path root = temp.resolve("shaders");
        Files.createDirectories(root);
        Files.writeString(root.resolve("main.vert"), "#version 330 core\n#include \"../../outside.glsl\"");

        try (URLClassLoader loader = new URLClassLoader(new URL[]{temp.toUri().toURL()}, null)) {
            assertThrows(IOException.class,
                    () -> new ShaderSourceLoader(loader, "shaders").load("main.vert"));
        }
    }

    @Test
    void requiresRootVersionDirective() throws Exception {
        Path temp = Files.createTempDirectory("aether-version");
        Path root = temp.resolve("shaders");
        Files.createDirectories(root);
        Files.writeString(root.resolve("main.vert"), "void main() {}");

        try (URLClassLoader loader = new URLClassLoader(new URL[]{temp.toUri().toURL()}, null)) {
            assertThrows(IOException.class,
                    () -> new ShaderSourceLoader(loader, "shaders").load("main.vert"));
        }
    }
}
