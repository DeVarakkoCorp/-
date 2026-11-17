package dev.aether.client.world.chunk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChunkWorldStorageTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesAndLoadsWorldWithNegativeChunkCoordinates() throws IOException {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(-1, 17, 32, 42);
        world.setBlock(15, 0, -16, 9);
        Path file = temporaryDirectory.resolve("nested/world.aether");

        ChunkWorldStorage.save(world, file);
        ChunkWorld loaded = ChunkWorldStorage.load(file);

        assertEquals(42, loaded.blockIdAt(-1, 17, 32));
        assertEquals(9, loaded.blockIdAt(15, 0, -16));
        assertEquals(2, loaded.loadedSectionCount());
    }

    @Test
    void detectsPayloadCorruption() throws IOException {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(1, 2, 3, 4);
        Path file = temporaryDirectory.resolve("world.aether");
        ChunkWorldStorage.save(world, file);
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length / 2] ^= 0x20;
        Files.write(file, bytes);

        assertThrows(IOException.class, () -> ChunkWorldStorage.load(file));
    }

    @Test
    void rejectsInvalidFileSize() throws IOException {
        Path file = temporaryDirectory.resolve("empty.aether");
        Files.write(file, new byte[3]);
        assertThrows(IOException.class, () -> ChunkWorldStorage.load(file));
    }
}
