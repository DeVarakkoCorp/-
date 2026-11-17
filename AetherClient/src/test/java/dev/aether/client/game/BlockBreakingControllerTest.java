package dev.aether.client.game;

import dev.aether.client.world.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockBreakingControllerTest {
    @Test
    void accumulatesHeldTimeUntilTargetIsReady() {
        BlockBreakingController breaking = new BlockBreakingController();
        BlockRaycaster.Hit target = hit(2, 1);

        assertFalse(breaking.advance(target, 0.1, 0.5));
        assertEquals(0.2, breaking.progress(), 1e-9);
        assertFalse(breaking.advance(target, 0.1, 0.5));
        assertFalse(breaking.advance(target, 0.1, 0.5));
        assertFalse(breaking.advance(target, 0.1, 0.5));
        assertTrue(breaking.advance(target, 0.1, 0.5));
        assertEquals(1.0, breaking.progress(), 1e-9);
    }

    @Test
    void changingTargetOrReleasingResetsProgressAndLargeFramesAreCapped() {
        BlockBreakingController breaking = new BlockBreakingController();
        assertFalse(breaking.advance(hit(2, 1), 0.5, 1.0));
        assertEquals(0.1, breaking.progress(), 1e-9);
        assertFalse(breaking.advance(hit(3, 1), 0.0, 1.0));
        assertEquals(0.0, breaking.progress(), 1e-9);
        breaking.advance(hit(3, 1), 0.1, 1.0);
        breaking.reset();
        assertEquals(0.0, breaking.progress());
    }

    @Test
    void rejectsInvalidTimingValues() {
        BlockBreakingController breaking = new BlockBreakingController();
        assertThrows(IllegalArgumentException.class, () -> breaking.advance(hit(1, 1), -0.1, 1.0));
        assertThrows(IllegalArgumentException.class, () -> breaking.advance(hit(1, 1), 0.1, 0.0));
    }

    @Test
    void differentBlockDefinitionsCanUseDifferentMiningTimes() {
        BlockBreakingController breaking = new BlockBreakingController();
        assertFalse(breaking.advance(hit(1, 1), 0.1, 0.5));
        assertFalse(breaking.advance(hit(1, 1), 0.1, 0.5));
        assertFalse(breaking.advance(hit(1, 1), 0.1, 0.5));
        assertFalse(breaking.advance(hit(1, 1), 0.1, 0.5));
        assertTrue(breaking.advance(hit(1, 1), 0.1, 0.5));

        breaking.reset();
        assertFalse(breaking.advance(hit(1, 2), 0.1, 1.5));
        assertEquals(1.0 / 15.0, breaking.progress(), 1e-9);
    }

    private static BlockRaycaster.Hit hit(int x, int id) {
        return new BlockRaycaster.Hit(new BlockPos(x, 0, 0), new BlockPos(x - 1, 0, 0),
                BlockRaycaster.Face.WEST, 1.0, id);
    }
}
