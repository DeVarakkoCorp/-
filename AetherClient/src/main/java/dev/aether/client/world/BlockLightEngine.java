package dev.aether.client.world;

import dev.aether.client.world.chunk.ChunkPos;
import dev.aether.client.world.chunk.ChunkSection;
import dev.aether.client.world.chunk.ChunkWorld;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Computes local emissive block light over a stable snapshot of loaded sections. */
public final class BlockLightEngine {
    private static final int[][] DIRECTIONS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    public LightMap compute(ChunkWorld world, BlockRegistry blocks) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(blocks, "blocks");
        Map<ChunkPos, ChunkSection> sections = world.snapshotSections();
        Map<BlockPos, Integer> levels = new HashMap<>();
        ArrayDeque<Node> queue = new ArrayDeque<>();

        for (Map.Entry<ChunkPos, ChunkSection> entry : sections.entrySet()) {
            ChunkPos chunk = entry.getKey();
            ChunkSection section = entry.getValue();
            int originX = chunk.originX();
            int originY = chunk.originY();
            int originZ = chunk.originZ();
            for (int y = 0; y < ChunkSection.EDGE; y++) {
                for (int z = 0; z < ChunkSection.EDGE; z++) {
                    for (int x = 0; x < ChunkSection.EDGE; x++) {
                        int blockId = section.getBlockId(x, y, z);
                        if (blockId == BlockDefinition.AIR_ID) continue;
                        int emission = blocks.get(blockId).lightEmission();
                        if (emission == 0) continue;
                        BlockPos position = new BlockPos(originX + x, originY + y, originZ + z);
                        levels.put(position, emission);
                        queue.addLast(new Node(position, emission));
                    }
                }
            }
        }

        while (!queue.isEmpty()) {
            Node node = queue.removeFirst();
            if (levels.getOrDefault(node.position, 0) != node.level) continue;
            int propagated = node.level - 1;
            if (propagated == 0) continue;

            for (int[] direction : DIRECTIONS) {
                BlockPos neighbor = node.position.offset(direction[0], direction[1], direction[2]);
                ChunkPos neighborChunk = ChunkPos.fromBlock(neighbor.x(), neighbor.y(), neighbor.z());
                if (!sections.containsKey(neighborChunk)) continue;
                int neighborId = blockIdAt(sections, neighbor, neighborChunk);
                if (blocks.get(neighborId).opaque()) continue;
                if (levels.getOrDefault(neighbor, 0) >= propagated) continue;
                levels.put(neighbor, propagated);
                queue.addLast(new Node(neighbor, propagated));
            }
        }
        return new LightMap(levels);
    }

    private static int blockIdAt(Map<ChunkPos, ChunkSection> sections, BlockPos position, ChunkPos chunk) {
        ChunkSection section = sections.get(chunk);
        if (section == null) return BlockDefinition.AIR_ID;
        return section.getBlockId(Math.floorMod(position.x(), ChunkSection.EDGE),
                Math.floorMod(position.y(), ChunkSection.EDGE),
                Math.floorMod(position.z(), ChunkSection.EDGE));
    }

    private record Node(BlockPos position, int level) {}
}
