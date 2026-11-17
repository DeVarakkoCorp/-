package dev.aether.client.game;

/** A stack of one block item type; block id zero is reserved for an empty slot. */
public record ItemStack(int itemId, int count) {
    public static final ItemStack EMPTY = new ItemStack(0, 0);
    public static final int MAX_STACK_SIZE = 64;

    public ItemStack {
        if (itemId < 0 || count < 0 || count > MAX_STACK_SIZE
                || (itemId == 0) != (count == 0)) {
            throw new IllegalArgumentException("Invalid item stack");
        }
    }
}
