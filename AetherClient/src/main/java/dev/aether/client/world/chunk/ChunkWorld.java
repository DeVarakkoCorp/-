package dev.aether.client.world.chunk;

import dev.aether.client.world.BlockAccess;
import dev.aether.client.world.BlockDefinition;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class ChunkWorld implements BlockAccess {
    private final Map<ChunkPos, ChunkSection> sections = new ConcurrentHashMap<>();
    private final AtomicLong revision = new AtomicLong();

    public ChunkSection getOrCreate(ChunkPos position) {
        return sections.computeIfAbsent(position, ignored -> new ChunkSection());
    }

    public ChunkSection get(ChunkPos position) {
        return sections.get(position);
    }

    public void putSection(ChunkPos position, ChunkSection section) {
        sections.put(position, section.snapshot());
        revision.incrementAndGet();
    }

    public Map<ChunkPos, ChunkSection> snapshotSections() {
        Map<ChunkPos, ChunkSection> copies = new HashMap<>();
        sections.forEach((position, section) -> copies.put(position, section.snapshot()));
        return Collections.unmodifiableMap(copies);
    }

    public boolean unload(ChunkPos position) {
        boolean removed = sections.remove(position) != null;
        if (removed) revision.incrementAndGet();
        return removed;
    }

    public long revision() { return revision.get(); }

    public int loadedSectionCount() {
        return sections.size();
    }

    public BlockAccess snapshot() {
        Map<ChunkPos, ChunkSection> immutableCopies = snapshotSections();
        return (worldX, worldY, worldZ) -> {
            ChunkPos position = ChunkPos.fromBlock(worldX, worldY, worldZ);
            ChunkSection section = immutableCopies.get(position);
            if (section == null) return BlockDefinition.AIR_ID;
            return section.getBlockId(Math.floorMod(worldX, ChunkSection.EDGE),
                    Math.floorMod(worldY, ChunkSection.EDGE),
                    Math.floorMod(worldZ, ChunkSection.EDGE));
        };
    }

    public void setBlock(int worldX, int worldY, int worldZ, int blockId) {
        if (blockId < 0) throw new IllegalArgumentException("Block id must not be negative");
        ChunkPos position = ChunkPos.fromBlock(worldX, worldY, worldZ);
        ChunkSection section = sections.get(position);
        if (section == null) {
            if (blockId == BlockDefinition.AIR_ID) return;
            section = getOrCreate(position);
        }
        int x = Math.floorMod(worldX, ChunkSection.EDGE);
        int y = Math.floorMod(worldY, ChunkSection.EDGE);
        int z = Math.floorMod(worldZ, ChunkSection.EDGE);
        synchronized (section) {
            if (section.getBlockId(x, y, z) == blockId) return;
            section.setBlockId(x, y, z, blockId);
            revision.incrementAndGet();
        }
    }

    @Override
    public int blockIdAt(int worldX, int worldY, int worldZ) {
        ChunkPos position = ChunkPos.fromBlock(worldX, worldY, worldZ);
        ChunkSection section = sections.get(position);
        if (section == null) return BlockDefinition.AIR_ID;
        return section.getBlockId(Math.floorMod(worldX, ChunkSection.EDGE),
                Math.floorMod(worldY, ChunkSection.EDGE),
                Math.floorMod(worldZ, ChunkSection.EDGE));
    }
}
