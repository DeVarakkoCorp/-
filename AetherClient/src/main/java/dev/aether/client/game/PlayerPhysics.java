package dev.aether.client.game;

import dev.aether.client.world.BlockAccess;
import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;

import java.util.Objects;

public final class PlayerPhysics {
    private static final double WIDTH = 0.6;
    private static final double HEIGHT = 1.8;
    private static final double MOVE_SPEED = 4.3;
    private static final double GRAVITY = 24.0;
    private static final double JUMP_SPEED = 8.0;
    private static final double MAX_SUBSTEP = 0.2;

    private final BlockRegistry blocks;

    public PlayerPhysics(BlockRegistry blocks) {
        this.blocks = Objects.requireNonNull(blocks, "blocks");
    }

    public void tick(PlayerState player, PlayerInput input, BlockAccess world, double deltaSeconds) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(world, "world");
        if (!Double.isFinite(deltaSeconds) || deltaSeconds <= 0.0 || deltaSeconds > 0.1) {
            throw new IllegalArgumentException("Physics step must be in (0, 0.1] seconds");
        }

        double strafe = input.strafe();
        double forward = input.forward();
        double length = Math.hypot(strafe, forward);
        if (length > 1.0) {
            strafe /= length;
            forward /= length;
        }
        player.velocityX = strafe * MOVE_SPEED;
        player.velocityZ = forward * MOVE_SPEED;
        if (input.jump() && player.onGround) {
            player.velocityY = JUMP_SPEED;
            player.onGround = false;
        }
        player.velocityY -= GRAVITY * deltaSeconds;

        moveAxis(player, world, 0, player.velocityX * deltaSeconds);
        moveAxis(player, world, 2, player.velocityZ * deltaSeconds);
        double verticalMove = player.velocityY * deltaSeconds;
        player.onGround = false;
        if (moveAxis(player, world, 1, verticalMove) && verticalMove < 0.0) player.onGround = true;
    }

    private boolean moveAxis(PlayerState player, BlockAccess world, int axis, double amount) {
        int steps = Math.max(1, (int) Math.ceil(Math.abs(amount) / MAX_SUBSTEP));
        double increment = amount / steps;
        for (int i = 0; i < steps; i++) {
            if (axis == 0) player.x += increment;
            else if (axis == 1) player.y += increment;
            else player.z += increment;
            if (collides(player, world)) {
                if (axis == 0) { player.x -= increment; player.velocityX = 0.0; }
                else if (axis == 1) { player.y -= increment; player.velocityY = 0.0; }
                else { player.z -= increment; player.velocityZ = 0.0; }
                return true;
            }
        }
        return false;
    }

    private boolean collides(PlayerState player, BlockAccess world) {
        double half = WIDTH * 0.5;
        int minX = floor(player.x - half + 1.0e-7);
        int maxX = floor(player.x + half - 1.0e-7);
        int minY = floor(player.y + 1.0e-7);
        int maxY = floor(player.y + HEIGHT - 1.0e-7);
        int minZ = floor(player.z - half + 1.0e-7);
        int maxZ = floor(player.z + half - 1.0e-7);
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    int id = world.blockIdAt(x, y, z);
                    if (id != BlockDefinition.AIR_ID && blocks.get(id).collidable()) return true;
                }
            }
        }
        return false;
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }
}
