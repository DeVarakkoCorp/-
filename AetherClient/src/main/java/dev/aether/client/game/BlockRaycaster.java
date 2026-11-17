package dev.aether.client.game;

import dev.aether.client.world.BlockAccess;
import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.BlockPos;

import java.util.Objects;
import java.util.Optional;

/** Grid DDA ray traversal for block selection and interaction. */
public final class BlockRaycaster {
    public Optional<Hit> cast(
            BlockAccess world,
            BlockRegistry blocks,
            double originX,
            double originY,
            double originZ,
            double directionX,
            double directionY,
            double directionZ,
            double reach
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(blocks, "blocks");
        if (!Double.isFinite(originX) || !Double.isFinite(originY) || !Double.isFinite(originZ)
                || !Double.isFinite(directionX) || !Double.isFinite(directionY) || !Double.isFinite(directionZ)
                || !Double.isFinite(reach) || reach <= 0.0) {
            throw new IllegalArgumentException("Ray values must be finite and reach must be positive");
        }
        double length = Math.hypot(Math.hypot(directionX, directionY), directionZ);
        if (length == 0.0) throw new IllegalArgumentException("Ray direction must not be zero");
        directionX /= length;
        directionY /= length;
        directionZ /= length;

        int x = floorToBlock(originX);
        int y = floorToBlock(originY);
        int z = floorToBlock(originZ);
        int blockId = world.blockIdAt(x, y, z);
        if (isCollidable(blocks, blockId)) {
            return Optional.of(new Hit(new BlockPos(x, y, z), new BlockPos(x, y, z),
                    Face.INSIDE, 0.0, blockId));
        }

        int stepX = sign(directionX);
        int stepY = sign(directionY);
        int stepZ = sign(directionZ);
        double deltaX = stepX == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / directionX);
        double deltaY = stepY == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / directionY);
        double deltaZ = stepZ == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / directionZ);
        double maxX = initialBoundary(originX, x, directionX, stepX);
        double maxY = initialBoundary(originY, y, directionY, stepY);
        double maxZ = initialBoundary(originZ, z, directionZ, stepZ);

        while (true) {
            double distance = Math.min(maxX, Math.min(maxY, maxZ));
            if (distance > reach) return Optional.empty();
            BlockPos previous = new BlockPos(x, y, z);
            boolean crossX = Double.compare(maxX, distance) == 0;
            boolean crossY = Double.compare(maxY, distance) == 0;
            boolean crossZ = Double.compare(maxZ, distance) == 0;
            Face enteredFace;
            if (crossX) {
                x = Math.addExact(x, stepX);
                maxX += deltaX;
                enteredFace = stepX > 0 ? Face.WEST : Face.EAST;
            } else if (crossY) {
                enteredFace = stepY > 0 ? Face.DOWN : Face.UP;
            } else {
                enteredFace = stepZ > 0 ? Face.NORTH : Face.SOUTH;
            }
            if (crossY) {
                y = Math.addExact(y, stepY);
                maxY += deltaY;
            }
            if (crossZ) {
                z = Math.addExact(z, stepZ);
                maxZ += deltaZ;
            }

            blockId = world.blockIdAt(x, y, z);
            if (isCollidable(blocks, blockId)) {
                return Optional.of(new Hit(new BlockPos(x, y, z), previous, enteredFace, distance, blockId));
            }
        }
    }

    private static boolean isCollidable(BlockRegistry blocks, int blockId) {
        return blockId != BlockDefinition.AIR_ID && blocks.get(blockId).collidable();
    }

    private static int floorToBlock(double coordinate) {
        double floored = Math.floor(coordinate);
        if (floored < Integer.MIN_VALUE || floored > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Ray origin lies outside supported world coordinates");
        }
        return (int) floored;
    }

    private static int sign(double value) {
        return value > 0.0 ? 1 : value < 0.0 ? -1 : 0;
    }

    private static double initialBoundary(double origin, int cell, double direction, int step) {
        if (step == 0) return Double.POSITIVE_INFINITY;
        double boundary = step > 0 ? cell + 1.0 : cell;
        return (boundary - origin) / direction;
    }

    public enum Face {
        WEST(-1, 0, 0), EAST(1, 0, 0), DOWN(0, -1, 0), UP(0, 1, 0),
        NORTH(0, 0, -1), SOUTH(0, 0, 1), INSIDE(0, 0, 0);

        private final int normalX;
        private final int normalY;
        private final int normalZ;

        Face(int normalX, int normalY, int normalZ) {
            this.normalX = normalX;
            this.normalY = normalY;
            this.normalZ = normalZ;
        }

        public int normalX() { return normalX; }
        public int normalY() { return normalY; }
        public int normalZ() { return normalZ; }
    }

    public record Hit(BlockPos block, BlockPos adjacent, Face face, double distance, int blockId) {
        public Hit {
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(adjacent, "adjacent");
            Objects.requireNonNull(face, "face");
            if (!Double.isFinite(distance) || distance < 0.0) {
                throw new IllegalArgumentException("Hit distance must be finite and non-negative");
            }
            if (blockId <= BlockDefinition.AIR_ID) throw new IllegalArgumentException("Hit block must not be air");
        }
    }
}
