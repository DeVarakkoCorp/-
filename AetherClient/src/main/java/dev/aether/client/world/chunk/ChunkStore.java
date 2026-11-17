package dev.aether.client.world.chunk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/** Persists individual chunk sections using the world's checksummed atomic file format. */
public final class ChunkStore {
    private final Path directory;

    public ChunkStore(Path directory) {
        this.directory = Objects.requireNonNull(directory, "directory").toAbsolutePath().normalize();
    }

    public void save(ChunkPos position, ChunkSection section) throws IOException {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(section, "section");
        ChunkWorld singleSection = new ChunkWorld();
        singleSection.putSection(position, section);
        ChunkWorldStorage.save(singleSection, pathFor(position));
    }

    public Optional<ChunkSection> load(ChunkPos position) throws IOException {
        Objects.requireNonNull(position, "position");
        Path path = pathFor(position);
        if (Files.notExists(path)) return Optional.empty();
        ChunkWorld stored = ChunkWorldStorage.load(path);
        if (stored.loadedSectionCount() != 1 || stored.get(position) == null) {
            throw new IOException("Chunk store entry does not match its key: " + position);
        }
        return Optional.of(stored.get(position));
    }

    public Path pathFor(ChunkPos position) {
        Objects.requireNonNull(position, "position");
        return directory.resolve(position.x() + "." + position.y() + "." + position.z() + ".aechunk");
    }
}
