package dev.aether.client.game;

/** Readable names for the prototype's stable item identifiers. */
public final class ItemNames {
    private ItemNames() {}

    public static String name(int itemId) {
        if (itemId < 0) throw new IllegalArgumentException("Item id must not be negative");
        return switch (itemId) {
            case 0 -> "empty";
            case 1 -> "Stone";
            case 2 -> "Polished Stone";
            default -> "Block-" + itemId;
        };
    }
}
