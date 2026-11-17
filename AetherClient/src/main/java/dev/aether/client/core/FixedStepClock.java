package dev.aether.client.core;

public final class FixedStepClock {
    private final double stepSeconds;
    private final int maxCatchUpSteps;
    private double accumulator;

    public FixedStepClock(double stepSeconds, int maxCatchUpSteps) {
        if (!Double.isFinite(stepSeconds) || stepSeconds <= 0.0) {
            throw new IllegalArgumentException("stepSeconds must be finite and positive");
        }
        if (maxCatchUpSteps <= 0) throw new IllegalArgumentException("maxCatchUpSteps must be positive");
        this.stepSeconds = stepSeconds;
        this.maxCatchUpSteps = maxCatchUpSteps;
    }

    public Frame advance(double frameSeconds) {
        if (!Double.isFinite(frameSeconds) || frameSeconds < 0.0) {
            throw new IllegalArgumentException("frameSeconds must be finite and non-negative");
        }
        accumulator += frameSeconds;
        int steps = Math.min((int) Math.floor(accumulator / stepSeconds), maxCatchUpSteps);
        accumulator -= steps * stepSeconds;

        double dropped = 0.0;
        if (accumulator >= stepSeconds) {
            dropped = accumulator - accumulator % stepSeconds;
            accumulator %= stepSeconds;
        }
        return new Frame(steps, accumulator / stepSeconds, dropped);
    }

    public double stepSeconds() {
        return stepSeconds;
    }

    public record Frame(int steps, double interpolation, double droppedSeconds) {}
}
