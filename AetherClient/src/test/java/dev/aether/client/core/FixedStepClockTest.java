package dev.aether.client.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixedStepClockTest {
    @Test
    void accumulatesFractionalFramesUntilOneSimulationStep() {
        FixedStepClock clock = new FixedStepClock(0.05, 4);
        assertEquals(0, clock.advance(0.02).steps());
        FixedStepClock.Frame frame = clock.advance(0.03);
        assertEquals(1, frame.steps());
        assertEquals(0.0, frame.interpolation(), 1.0e-9);
    }

    @Test
    void capsCatchUpStepsAndReportsDroppedTime() {
        FixedStepClock clock = new FixedStepClock(0.05, 3);
        FixedStepClock.Frame frame = clock.advance(1.0);
        assertEquals(3, frame.steps());
        assertTrue(frame.droppedSeconds() >= 0.8);
        assertTrue(frame.interpolation() >= 0.0 && frame.interpolation() < 1.0);
    }

    @Test
    void rejectsInvalidTimingValues() {
        assertThrows(IllegalArgumentException.class, () -> new FixedStepClock(0, 2));
        assertThrows(IllegalArgumentException.class, () -> new FixedStepClock(0.05, 0));
        FixedStepClock clock = new FixedStepClock(0.05, 2);
        assertThrows(IllegalArgumentException.class, () -> clock.advance(-0.1));
    }
}
