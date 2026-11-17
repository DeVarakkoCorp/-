package dev.aether.client.game;

import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.RenderLayer;
import dev.aether.client.world.chunk.ChunkWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerPhysicsTest {
    private final BlockRegistry blocks = BlockRegistry.builder()
            .register(new BlockDefinition(1, RenderLayer.OPAQUE, true, true))
            .build();
    private final PlayerPhysics physics = new PlayerPhysics(blocks);

    @Test
    void gravitySettlesPlayerOnSolidFloorAndJumpMovesUpward() {
        ChunkWorld world = floorWorld();
        PlayerState player = new PlayerState(0.5, 2.0, 0.5);
        for (int i = 0; i < 80; i++) physics.tick(player, new PlayerInput(0, 0, false), world, 0.05);
        assertTrue(player.onGround());
        assertEquals(1.0, player.y(), 0.05);
        physics.tick(player, new PlayerInput(0, 0, true), world, 0.05);
        assertFalse(player.onGround());
        assertTrue(player.y() > 1.04);
    }

    @Test
    void horizontalMovementStopsAtWall() {
        ChunkWorld world = floorWorld();
        for (int y = 1; y <= 3; y++) world.setBlock(2, y, 0, 1);
        PlayerState player = new PlayerState(0.5, 1.0, 0.5);
        for (int i = 0; i < 20; i++) physics.tick(player, new PlayerInput(1, 0, false), world, 0.05);
        assertTrue(player.x() < 1.71);
    }

    @Test
    void rejectsInvalidStepDuration() {
        assertThrows(IllegalArgumentException.class,
                () -> physics.tick(new PlayerState(0, 1, 0), new PlayerInput(0, 0, false),
                        new ChunkWorld(), 0.2));
    }

    private ChunkWorld floorWorld() {
        ChunkWorld world = new ChunkWorld();
        for (int z = -2; z <= 2; z++) {
            for (int x = -2; x <= 2; x++) world.setBlock(x, 0, z, 1);
        }
        return world;
    }
}
