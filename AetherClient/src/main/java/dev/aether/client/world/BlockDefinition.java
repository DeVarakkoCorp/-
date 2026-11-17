package dev.aether.client.world;

import java.util.Objects;

public record BlockDefinition(
        int id,
        RenderLayer renderLayer,
        boolean opaque,
        boolean collidable,
        int lightEmission,
        double miningSeconds
) {
    public static final int AIR_ID = 0;
    public static final BlockDefinition AIR = new BlockDefinition(AIR_ID, RenderLayer.OPAQUE, false, false, 0, 0.0);

    public BlockDefinition(int id, RenderLayer renderLayer, boolean opaque, boolean collidable) {
        this(id, renderLayer, opaque, collidable, 0, defaultMiningSeconds(id));
    }

    public BlockDefinition(int id, RenderLayer renderLayer, boolean opaque, boolean collidable, int lightEmission) {
        this(id, renderLayer, opaque, collidable, lightEmission, defaultMiningSeconds(id));
    }

    public BlockDefinition {
        if (id < 0) throw new IllegalArgumentException("Block id must not be negative");
        Objects.requireNonNull(renderLayer, "renderLayer");
        if (lightEmission < 0 || lightEmission > 15) {
            throw new IllegalArgumentException("Block light emission must be between 0 and 15");
        }
        if (!Double.isFinite(miningSeconds) || miningSeconds < 0.0
                || (id == AIR_ID ? miningSeconds != 0.0 : miningSeconds <= 0.0)) {
            throw new IllegalArgumentException("Air mining time must be zero; solid block mining time must be positive");
        }
        if (id == AIR_ID && (opaque || collidable)) {
            throw new IllegalArgumentException("Air cannot be opaque or collidable");
        }
        if (opaque && renderLayer != RenderLayer.OPAQUE) {
            throw new IllegalArgumentException("Only opaque-layer blocks may occlude neighboring faces");
        }
    }

    private static double defaultMiningSeconds(int id) {
        return id == AIR_ID ? 0.0 : 0.5;
    }
}
