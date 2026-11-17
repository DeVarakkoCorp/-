package dev.aether.client.game;

import java.util.Arrays;
import java.util.Objects;

/** A four-cell crafting grid with the prototype recipe: four stone blocks make one polished stone block. */
public final class CraftingGrid {
    public static final int SLOT_COUNT = 4;
    public static final int STONE_ID = 1;
    public static final int POLISHED_STONE_ID = 2;
    private final ItemStack[] inputs = new ItemStack[SLOT_COUNT];

    public CraftingGrid() {
        Arrays.fill(inputs, ItemStack.EMPTY);
    }

    public ItemStack input(int slot) {
        checkSlot(slot);
        return inputs[slot];
    }

    public ItemStack result() {
        for (ItemStack input : inputs) {
            if (input.itemId() != STONE_ID || input.count() < 1) return ItemStack.EMPTY;
        }
        return new ItemStack(POLISHED_STONE_ID, 1);
    }

    /** Moves one item into a selected crafting cell only when both source and destination can accept it. */
    public boolean addOne(PlayerInventory inventory, int inventorySlot, int gridSlot) {
        Objects.requireNonNull(inventory, "inventory");
        checkSlot(gridSlot);
        ItemStack source = inventory.slot(inventorySlot);
        ItemStack target = inputs[gridSlot];
        if (source.count() == 0 || (target.count() > 0 && target.itemId() != source.itemId())
                || target.count() >= ItemStack.MAX_STACK_SIZE) return false;
        if (!removeFromSlot(inventory, inventorySlot, 1)) return false;
        inputs[gridSlot] = new ItemStack(source.itemId(), target.count() + 1);
        return true;
    }

    public boolean craft(PlayerInventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        ItemStack output = result();
        if (output.count() == 0 || !inventory.canAddItems(output.itemId(), output.count())) return false;
        for (int slot = 0; slot < inputs.length; slot++) {
            ItemStack input = inputs[slot];
            inputs[slot] = input.count() == 1 ? ItemStack.EMPTY : new ItemStack(input.itemId(), input.count() - 1);
        }
        if (inventory.addItems(output.itemId(), output.count()) != 0) {
            throw new IllegalStateException("Inventory capacity changed during crafting");
        }
        return true;
    }

    /** Returns buffered ingredients. A partially returned stack remains here for a later retry. */
    public boolean returnItems(PlayerInventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        boolean empty = true;
        for (int slot = 0; slot < inputs.length; slot++) {
            ItemStack input = inputs[slot];
            if (input.count() == 0) continue;
            int remaining = inventory.addItems(input.itemId(), input.count());
            inputs[slot] = remaining == 0 ? ItemStack.EMPTY
                    : new ItemStack(input.itemId(), remaining);
            empty &= remaining == 0;
        }
        return empty;
    }

    public int[] itemIds() {
        int[] ids = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) ids[i] = inputs[i].itemId();
        return ids;
    }

    public int[] counts() {
        int[] counts = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) counts[i] = inputs[i].count();
        return counts;
    }

    private static boolean removeFromSlot(PlayerInventory inventory, int slot, int amount) {
        ItemStack current = inventory.slot(slot);
        if (current.count() < amount) return false;
        inventory.setSlot(slot, current.count() == amount ? ItemStack.EMPTY
                : new ItemStack(current.itemId(), current.count() - amount));
        return true;
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) throw new IndexOutOfBoundsException("Crafting slot: " + slot);
    }
}
