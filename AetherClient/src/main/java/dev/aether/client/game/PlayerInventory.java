package dev.aether.client.game;

import java.util.Arrays;
import java.util.Objects;

/** Minimal 36-slot inventory with a selected nine-slot hotbar. */
public final class PlayerInventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int SLOT_COUNT = 36;
    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];
    private int selectedHotbarSlot;

    public PlayerInventory() {
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    public ItemStack slot(int index) {
        checkSlot(index);
        return slots[index];
    }

    public void setSlot(int index, ItemStack stack) {
        checkSlot(index);
        slots[index] = Objects.requireNonNull(stack, "stack");
    }

    public void swapSlots(int first, int second) {
        checkSlot(first);
        checkSlot(second);
        if (first == second) return;
        ItemStack stack = slots[first];
        slots[first] = slots[second];
        slots[second] = stack;
    }

    public int selectedHotbarSlot() { return selectedHotbarSlot; }

    public void selectHotbarSlot(int index) {
        if (index < 0 || index >= HOTBAR_SIZE) throw new IllegalArgumentException("Hotbar slot must be 0..8");
        selectedHotbarSlot = index;
    }

    public ItemStack selectedStack() { return slots[selectedHotbarSlot]; }

    public boolean[] hotbarOccupancy() {
        boolean[] occupied = new boolean[HOTBAR_SIZE];
        for (int slot = 0; slot < HOTBAR_SIZE; slot++) occupied[slot] = slots[slot].count() > 0;
        return occupied;
    }

    public int addItems(int itemId, int amount) {
        if (itemId <= 0 || amount < 0) throw new IllegalArgumentException("Invalid item id or amount");
        int remaining = amount;
        for (int index = 0; index < slots.length && remaining > 0; index++) {
            ItemStack current = slots[index];
            if (current.itemId() != itemId || current.count() == ItemStack.MAX_STACK_SIZE) continue;
            int moved = Math.min(remaining, ItemStack.MAX_STACK_SIZE - current.count());
            slots[index] = new ItemStack(itemId, current.count() + moved);
            remaining -= moved;
        }
        for (int index = 0; index < slots.length && remaining > 0; index++) {
            if (slots[index].count() != 0) continue;
            int moved = Math.min(remaining, ItemStack.MAX_STACK_SIZE);
            slots[index] = new ItemStack(itemId, moved);
            remaining -= moved;
        }
        return remaining;
    }

    public boolean canAddItems(int itemId, int amount) {
        if (itemId <= 0 || amount < 0) throw new IllegalArgumentException("Invalid item id or amount");
        long capacity = 0;
        for (ItemStack stack : slots) {
            capacity += stack.count() == 0 ? ItemStack.MAX_STACK_SIZE
                    : stack.itemId() == itemId ? ItemStack.MAX_STACK_SIZE - stack.count() : 0;
        }
        return amount <= capacity;
    }

    public boolean removeFromSelected(int amount) {
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
        ItemStack selected = selectedStack();
        if (selected.count() < amount) return false;
        slots[selectedHotbarSlot] = selected.count() == amount
                ? ItemStack.EMPTY : new ItemStack(selected.itemId(), selected.count() - amount);
        return true;
    }

    private static void checkSlot(int index) {
        if (index < 0 || index >= SLOT_COUNT) throw new IndexOutOfBoundsException("Inventory slot: " + index);
    }
}
