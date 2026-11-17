package dev.aether.client.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ItemNamesTest {
    @Test
    void namesKnownItemsAndKeepsUnknownIdsReadable() {
        assertEquals("empty", ItemNames.name(0));
        assertEquals("Stone", ItemNames.name(1));
        assertEquals("Polished Stone", ItemNames.name(2));
        assertEquals("Block-79", ItemNames.name(79));
        assertThrows(IllegalArgumentException.class, () -> ItemNames.name(-1));
    }
}
