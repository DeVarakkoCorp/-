package dev.aether.client.game;

import java.util.Objects;

/** Tracks held-button mining progress for a stable block target. */
public final class BlockBreakingController {
    private static final double MAX_FRAME_DELTA_SECONDS = 0.1;
    private dev.aether.client.world.BlockPos target;
    private int targetBlockId;
    private double elapsedSeconds;
    private double requiredSeconds;

    public boolean advance(BlockRaycaster.Hit hit, double deltaSeconds, double breakSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0.0
                || !Double.isFinite(breakSeconds) || breakSeconds <= 0.0) {
            throw new IllegalArgumentException("Mining time values must be finite and non-negative/positive");
        }
        Objects.requireNonNull(hit, "hit");
        if (!hit.block().equals(target) || hit.blockId() != targetBlockId) {
            target = hit.block();
            targetBlockId = hit.blockId();
            elapsedSeconds = 0.0;
            requiredSeconds = breakSeconds;
        }
        elapsedSeconds = Math.min(requiredSeconds,
                elapsedSeconds + Math.min(deltaSeconds, MAX_FRAME_DELTA_SECONDS));
        return elapsedSeconds >= requiredSeconds;
    }

    public double progress() {
        return target == null || requiredSeconds <= 0.0 ? 0.0 : elapsedSeconds / requiredSeconds;
    }

    public void reset() {
        target = null;
        targetBlockId = 0;
        elapsedSeconds = 0.0;
        requiredSeconds = 0.0;
    }
}
