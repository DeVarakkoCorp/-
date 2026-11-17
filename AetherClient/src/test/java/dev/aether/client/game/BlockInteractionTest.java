package dev.aether.client.game;

import dev.aether.client.world.BlockPos;
import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.RenderLayer;
import dev.aether.client.world.chunk.ChunkWorld;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockInteractionTest {
    private static final BlockRegistry BLOCKS = BlockRegistry.builder()
            .register(new BlockDefinition(1, RenderLayer.OPAQUE, true, true))
            .register(new BlockDefinition(2, RenderLayer.OPAQUE, true, true))
            .build();
    private final BlockInteraction interaction = new BlockInteraction();

    @Test
    void minesTheBlockOnlyWhenTheRayHitStillMatchesTheWorld() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(2, 1, 0, 1);
        BlockRaycaster.Hit hit = hit(new BlockPos(2, 1, 0), new BlockPos(1, 1, 0), 1);

        assertTrue(interaction.mine(world, BLOCKS, hit));
        assertEquals(BlockDefinition.AIR_ID, world.blockIdAt(2, 1, 0));
        assertFalse(interaction.mine(world, BLOCKS, hit));
    }

    @Test
    void placesIntoTheAdjacentAirBlockAndUpdatesWorldRevision() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(2, 1, 0, 1);
        long revision = world.revision();
        BlockRaycaster.Hit hit = hit(new BlockPos(2, 1, 0), new BlockPos(1, 1, 0), 1);

        assertTrue(interaction.place(world, BLOCKS, hit, 2));
        assertEquals(2, world.blockIdAt(1, 1, 0));
        assertEquals(revision + 1, world.revision());
    }

    @Test
    void rejectsPlacementOnOccupiedCellInsideHitOrUnregisteredBlock() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(2, 1, 0, 1);
        world.setBlock(1, 1, 0, 1);
        BlockRaycaster.Hit hit = hit(new BlockPos(2, 1, 0), new BlockPos(1, 1, 0), 1);

        assertFalse(interaction.place(world, BLOCKS, hit, 2));
        assertFalse(interaction.place(world, BLOCKS,
                new BlockRaycaster.Hit(hit.block(), hit.block(), BlockRaycaster.Face.INSIDE, 0, 1), 2));
        assertThrows(IllegalArgumentException.class, () -> interaction.place(world, BLOCKS, hit, 99));
    }

    @Test
    void willNotPlaceABlockInsideThePlayerBoundingBox() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(2, 1, 0, 1);
        BlockRaycaster.Hit hit = hit(new BlockPos(2, 1, 0), new BlockPos(1, 1, 0), 1);
        PlayerState player = new PlayerState(1.5, 1.0, 0.5);

        assertFalse(interaction.place(world, BLOCKS, hit, 2, player));
        assertEquals(BlockDefinition.AIR_ID, world.blockIdAt(1, 1, 0));
    }

    @Test
    void miningAddsTheBlockToInventoryAndRejectsMiningWhenInventoryIsFull() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(2, 1, 0, 1);
        BlockRaycaster.Hit hit = hit(new BlockPos(2, 1, 0), new BlockPos(1, 1, 0), 1);
        PlayerInventory inventory = new PlayerInventory();

        assertTrue(interaction.mine(world, BLOCKS, hit, inventory));
        assertEquals(new ItemStack(1, 1), inventory.selectedStack());
        world.setBlock(2, 1, 0, 1);
        for (int slot = 0; slot < PlayerInventory.SLOT_COUNT; slot++) {
            inventory.setSlot(slot, new ItemStack(2, ItemStack.MAX_STACK_SIZE));
        }

        assertFalse(interaction.mine(world, BLOCKS, hit, inventory));
        assertEquals(1, world.blockIdAt(2, 1, 0));
    }

    @Test
    void placementConsumesSelectedItemOnlyAfterSuccessfulWorldEdit() {
        ChunkWorld world = new ChunkWorld();
        world.setBlock(2, 1, 0, 1);
        BlockRaycaster.Hit hit = hit(new BlockPos(2, 1, 0), new BlockPos(1, 1, 0), 1);
        PlayerInventory inventory = new PlayerInventory();
        inventory.setSlot(0, new ItemStack(2, 1));

        assertTrue(interaction.place(world, BLOCKS, hit, inventory, null));
        assertEquals(2, world.blockIdAt(1, 1, 0));
        assertEquals(ItemStack.EMPTY, inventory.selectedStack());
        assertFalse(interaction.place(world, BLOCKS, hit, inventory, null));
    }

    private static BlockRaycaster.Hit hit(BlockPos block, BlockPos adjacent, int blockId) {
        return new BlockRaycaster.Hit(block, adjacent, BlockRaycaster.Face.WEST, 1.0, blockId);
    }
}
