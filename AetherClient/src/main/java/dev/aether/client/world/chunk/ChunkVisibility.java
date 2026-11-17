package dev.aether.client.world.chunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Selects loaded horizontal chunks and bounded generation columns around a player. */
public final class ChunkVisibility {
    private ChunkVisibility() {}

    public static List<ChunkPos> withinRadius(
            Map<ChunkPos, ChunkSection> loadedSections,
            int playerBlockX,
            int playerBlockZ,
            int radiusInChunks
    ) {
        Objects.requireNonNull(loadedSections, "loadedSections");
        validateRadius(radiusInChunks);
        ChunkPos center = ChunkPos.fromBlock(playerBlockX, 0, playerBlockZ);
        return loadedSections.keySet().stream()
                .filter(position -> Math.abs((long) position.x() - center.x()) <= radiusInChunks
                        && Math.abs((long) position.z() - center.z()) <= radiusInChunks)
                .sorted(Comparator.comparingInt(ChunkPos::z)
                        .thenComparingInt(ChunkPos::x)
                        .thenComparingInt(ChunkPos::y))
                .toList();
    }

    public static List<ChunkPos> columnsAround(int playerBlockX, int playerBlockZ, int radiusInChunks) {
        validateRadius(radiusInChunks);
        ChunkPos center = ChunkPos.fromBlock(playerBlockX, 0, playerBlockZ);
        List<ChunkPos> positions = new ArrayList<>((radiusInChunks * 2 + 1) * (radiusInChunks * 2 + 1));
        for (int z = -radiusInChunks; z <= radiusInChunks; z++) {
            for (int x = -radiusInChunks; x <= radiusInChunks; x++) {
                positions.add(new ChunkPos(Math.addExact(center.x(), x), 0,
                        Math.addExact(center.z(), z)));
            }
        }
        return List.copyOf(positions);
    }

    private static void validateRadius(int radiusInChunks) {
        if (radiusInChunks < 0 || radiusInChunks > 32) {
            throw new IllegalArgumentException("Chunk visibility radius must be between 0 and 32");
        }
    }
}
