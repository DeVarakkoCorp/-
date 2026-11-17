package dev.aether.client.world;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable light snapshot. Unstored positions use the map's default level. */
public final class LightMap {
    public static final LightMap DARK = new LightMap(Map.of(), 0);
    public static final LightMap FULL_BRIGHT = new LightMap(Map.of(), 15);

    private final Map<BlockPos, Integer> levels;
    private final int defaultLevel;

    LightMap(Map<BlockPos, Integer> levels) {
        this(levels, 0);
    }

    private LightMap(Map<BlockPos, Integer> levels, int defaultLevel) {
        Objects.requireNonNull(levels, "levels");
        if (defaultLevel < 0 || defaultLevel > 15) throw new IllegalArgumentException("Invalid default light level");
        for (Map.Entry<BlockPos, Integer> entry : levels.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "light position");
            Integer level = Objects.requireNonNull(entry.getValue(), "light level");
            if (level < 1 || level > 15) throw new IllegalArgumentException("Light level must be between 1 and 15");
        }
        this.levels = Map.copyOf(levels);
        this.defaultLevel = defaultLevel;
    }

    public int levelAt(int x, int y, int z) {
        return levels.getOrDefault(new BlockPos(x, y, z), defaultLevel);
    }

    public int litBlockCount() {
        return levels.size();
    }

    public static LightMap maximum(LightMap first, LightMap second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        int defaultValue = Math.max(first.defaultLevel, second.defaultLevel);
        Map<BlockPos, Integer> combined = new HashMap<>();
        first.levels.forEach((position, level) -> {
            int maximum = Math.max(level, second.levelAt(position.x(), position.y(), position.z()));
            if (maximum > defaultValue) combined.put(position, maximum);
        });
        second.levels.forEach((position, level) -> {
            int maximum = Math.max(level, first.levelAt(position.x(), position.y(), position.z()));
            if (maximum > defaultValue) combined.put(position, maximum);
        });
        return new LightMap(combined, defaultValue);
    }
}
