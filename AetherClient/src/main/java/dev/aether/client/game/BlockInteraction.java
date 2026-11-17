package dev.aether.client.game;

import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.chunk.ChunkWorld;

import java.util.Objects;

/** Applies mining and placement actions from a previously computed ray hit. */
public final class BlockInteraction {
    public boolean mine(ChunkWorld world, BlockRegistry blocks, BlockRaycaster.Hit hit) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(hit, "hit");
        int currentId = world.blockIdAt(hit.block().x(), hit.block().y(), hit.block().z());
        if (currentId != hit.blockId() || !blocks.get(currentId).collidable()) return false;
        world.setBlock(hit.block().x(), hit.block().y(), hit.block().z(), BlockDefinition.AIR_ID);
        return true;
    }

    public boolean mine(ChunkWorld world, BlockRegistry blocks, BlockRaycaster.Hit hit,
                        PlayerInventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(hit, "hit");
        int currentId = world.blockIdAt(hit.block().x(), hit.block().y(), hit.block().z());
        if (currentId != hit.blockId() || !blocks.get(currentId).collidable()
                || !inventory.canAddItems(currentId, 1)) return false;
        world.setBlock(hit.block().x(), hit.block().y(), hit.block().z(), BlockDefinition.AIR_ID);
        if (inventory.addItems(currentId, 1) != 0) {
            throw new IllegalStateException("Inventory capacity changed during block mining");
        }
        return true;
    }

    public boolean place(ChunkWorld world, BlockRegistry blocks, BlockRaycaster.Hit hit, int blockId) {
        return place(world, blocks, hit, blockId, null);
    }

    public boolean place(
            ChunkWorld world,
            BlockRegistry blocks,
            BlockRaycaster.Hit hit,
            int blockId,
            PlayerState player
    ) {
        Objects.requireNonNull(world, "world");
        Objects.requireNonNull(blocks, "blocks");
        Objects.requireNonNull(hit, "hit");
        if (blockId == BlockDefinition.AIR_ID || hit.face() == BlockRaycaster.Face.INSIDE) return false;
        blocks.get(blockId);
        int hitId = world.blockIdAt(hit.block().x(), hit.block().y(), hit.block().z());
        if (hitId != hit.blockId() || !blocks.get(hitId).collidable()) return false;
        int adjacentId = world.blockIdAt(hit.adjacent().x(), hit.adjacent().y(), hit.adjacent().z());
        if (adjacentId != BlockDefinition.AIR_ID) return false;
        if (player != null && intersectsPlayer(hit.adjacent(), player)) return false;
        world.setBlock(hit.adjacent().x(), hit.adjacent().y(), hit.adjacent().z(), blockId);
        return true;
    }

    public boolean place(ChunkWorld world, BlockRegistry blocks, BlockRaycaster.Hit hit,
                         PlayerInventory inventory, PlayerState player) {
        Objects.requireNonNull(inventory, "inventory");
        ItemStack selected = inventory.selectedStack();
        if (selected.count() == 0 || !place(world, blocks, hit, selected.itemId(), player)) return false;
        if (!inventory.removeFromSelected(1)) {
            throw new IllegalStateException("Selected stack changed during block placement");
        }
        return true;
    }

    private static boolean intersectsPlayer(dev.aether.client.world.BlockPos block, PlayerState player) {
        double halfWidth = 0.3;
        double playerMaxY = player.y() + 1.8;
        return player.x() + halfWidth > block.x() && player.x() - halfWidth < block.x() + 1.0
                && playerMaxY > block.y() && player.y() < block.y() + 1.0
                && player.z() + halfWidth > block.z() && player.z() - halfWidth < block.z() + 1.0;
    }
}
