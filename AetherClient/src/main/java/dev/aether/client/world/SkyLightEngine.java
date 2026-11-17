package dev.aether.client.world;

import dev.aether.client.world.chunk.ChunkPos;
import dev.aether.client.world.chunk.ChunkSection;
import dev.aether.client.world.chunk.ChunkWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Computes direct top-down sky light for loaded vertical section columns. */
public final class SkyLightEngine {
    public LightMap compute(ChunkWorld world, BlockRegistry blocks) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(blocks, "blocks");
        Map<ChunkPos, ChunkSection> sections = world.snapshotSections();
        Map<Column, List<Map.Entry<ChunkPos, ChunkSection>>> columns = new HashMap<>();
        for (Map.Entry<ChunkPos, ChunkSection> entry : sections.entrySet()) {
            ChunkPos position = entry.getKey();
            columns.computeIfAbsent(new Column(position.x(), position.z()), ignored -> new ArrayList<>())
                    .add(entry);
        }

        Map<BlockPos, Integer> levels = new HashMap<>();
        for (List<Map.Entry<ChunkPos, ChunkSection>> column : columns.values()) {
            column.sort(Comparator.comparingInt((Map.Entry<ChunkPos, ChunkSection> entry) -> entry.getKey().y())
                    .reversed());
            for (int z = 0; z < ChunkSection.EDGE; z++) {
                for (int x = 0; x < ChunkSection.EDGE; x++) {
                    int light = 15;
                    int previousSectionY = Integer.MIN_VALUE;
                    for (Map.Entry<ChunkPos, ChunkSection> entry : column) {
                        ChunkPos chunk = entry.getKey();
                        ChunkSection section = entry.getValue();
                        if (previousSectionY != Integer.MIN_VALUE && previousSectionY - chunk.y() != 1) {
                            light = 15;
                        }
                        for (int y = ChunkSection.EDGE - 1; y >= 0; y--) {
                            int blockId = section.getBlockId(x, y, z);
                            BlockDefinition block = blockId == BlockDefinition.AIR_ID
                                    ? BlockDefinition.AIR : blocks.get(blockId);
                            if (block.opaque()) {
                                light = 0;
                                continue;
                            }
                            if (light > 0) {
                                levels.put(new BlockPos(chunk.originX() + x, chunk.originY() + y,
                                        chunk.originZ() + z), light);
                                if (blockId != BlockDefinition.AIR_ID) light = Math.max(0, light - 1);
                            }
                        }
                        previousSectionY = chunk.y();
                    }
                }
            }
        }
        return new LightMap(levels);
    }

    private record Column(int x, int z) {}
}
