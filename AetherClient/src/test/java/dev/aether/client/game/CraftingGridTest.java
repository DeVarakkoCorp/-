package dev.aether.client.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CraftingGridTest {
    @Test
    void craftsFourStoneInputsIntoOnePolishedBlock() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.setSlot(0, new ItemStack(CraftingGrid.STONE_ID, 8));
        CraftingGrid grid = new CraftingGrid();

        for (int slot = 0; slot < CraftingGrid.SLOT_COUNT; slot++) {
            assertTrue(grid.addOne(inventory, 0, slot));
        }
        assertEquals(new ItemStack(CraftingGrid.POLISHED_STONE_ID, 1), grid.result());
        assertEquals(new ItemStack(CraftingGrid.STONE_ID, 4), inventory.slot(0));
        assertTrue(grid.craft(inventory));
        assertEquals(new ItemStack(CraftingGrid.POLISHED_STONE_ID, 1), inventory.slot(1));
        assertEquals(ItemStack.EMPTY, grid.result());
    }

    @Test
    void rejectsIncompleteAndNonStoneRecipesWithoutConsumingInputs() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.setSlot(0, new ItemStack(7, 1));
        CraftingGrid grid = new CraftingGrid();

        assertTrue(grid.addOne(inventory, 0, 0));
        assertEquals(ItemStack.EMPTY, grid.result());
        assertFalse(grid.craft(inventory));
        assertEquals(new ItemStack(7, 1), grid.input(0));
    }

    @Test
    void returnsBufferedItemsToInventoryWhenClosingTheScreen() {
        PlayerInventory inventory = new PlayerInventory();
        inventory.setSlot(0, new ItemStack(CraftingGrid.STONE_ID, 1));
        CraftingGrid grid = new CraftingGrid();

        assertTrue(grid.addOne(inventory, 0, 0));
        assertTrue(grid.returnItems(inventory));
        assertEquals(new ItemStack(CraftingGrid.STONE_ID, 1), inventory.slot(0));
        assertEquals(ItemStack.EMPTY, grid.input(0));
    }

    @Test
    void doesNotConsumeRecipeWhenOutputCannotFit() {
        PlayerInventory inventory = new PlayerInventory();
        for (int slot = 0; slot < PlayerInventory.SLOT_COUNT; slot++) {
            inventory.setSlot(slot, new ItemStack(10 + slot, 64));
        }
        inventory.setSlot(0, new ItemStack(CraftingGrid.STONE_ID, 64));
        CraftingGrid grid = new CraftingGrid();
        for (int slot = 0; slot < 4; slot++) assertTrue(grid.addOne(inventory, 0, slot));

        assertFalse(grid.craft(inventory));
        assertEquals(new ItemStack(CraftingGrid.STONE_ID, 60), inventory.slot(0));
        for (int slot = 0; slot < 4; slot++) assertEquals(new ItemStack(CraftingGrid.STONE_ID, 1), grid.input(slot));
    }
}
