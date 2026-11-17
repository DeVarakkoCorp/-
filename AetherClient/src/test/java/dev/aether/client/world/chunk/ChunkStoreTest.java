package dev.aether.client.world.chunk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ChunkStoreTest {
    @TempDir
    Path directory;

    @Test
    void savesAndLoadsASectionByItsChunkPosition() throws IOException {
        ChunkStore store = new ChunkStore(directory);
        ChunkPos position = new ChunkPos(-2, 0, 3);
        ChunkSection section = new ChunkSection();
        section.setBlockId(15, 7, 1, 4);

        assertTrue(store.load(position).isEmpty());
        store.save(position, section);

        ChunkSection loaded = store.load(position).orElseThrow();
        assertEquals(4, loaded.getBlockId(15, 7, 1));
        assertTrue(Files.exists(store.pathFor(position)));
    }

    @Test
    void rejectsCorruptChunkFiles() throws IOException {
        ChunkStore store = new ChunkStore(directory);
        ChunkPos position = new ChunkPos(1, 0, -1);
        store.save(position, new ChunkSection());
        byte[] bytes = Files.readAllBytes(store.pathFor(position));
        bytes[20] ^= 1;
        Files.write(store.pathFor(position), bytes);

        assertThrows(IOException.class, () -> store.load(position));
    }
}
