package dev.aether.client.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PlayerProfileStorageTest {
    @TempDir
    Path directory;

    @Test
    void roundTripsPlayerPositionAndEveryInventorySlot() throws IOException {
        Path file = directory.resolve("player.aether");
        PlayerInventory inventory = new PlayerInventory();
        inventory.selectHotbarSlot(4);
        inventory.setSlot(4, new ItemStack(1, 27));
        inventory.setSlot(19, new ItemStack(3, 8));

        PlayerProfileStorage.save(file, new PlayerState(-12.5, 64.0, 8.25), inventory);
        PlayerProfileStorage.PlayerProfile loaded = PlayerProfileStorage.load(file).orElseThrow();

        assertEquals(-12.5, loaded.player().x());
        assertEquals(64.0, loaded.player().y());
        assertEquals(8.25, loaded.player().z());
        assertEquals(4, loaded.inventory().selectedHotbarSlot());
        assertEquals(new ItemStack(1, 27), loaded.inventory().slot(4));
        assertEquals(new ItemStack(3, 8), loaded.inventory().slot(19));
        assertEquals(ItemStack.EMPTY, loaded.inventory().slot(0));
    }

    @Test
    void returnsEmptyForMissingProfileAndRejectsCorruptProfiles() throws IOException {
        Path file = directory.resolve("player.aether");
        assertTrue(PlayerProfileStorage.load(file).isEmpty());
        PlayerProfileStorage.save(file, new PlayerState(1.0, 2.0, 3.0), new PlayerInventory());
        byte[] bytes = Files.readAllBytes(file);
        bytes[20] ^= 1;
        Files.write(file, bytes);

        assertThrows(IOException.class, () -> PlayerProfileStorage.load(file));
    }

    @Test
    void rejectsInvalidProfileLengths() throws IOException {
        Path file = directory.resolve("player.aether");
        Files.write(file, new byte[]{1, 2, 3});
        assertThrows(IOException.class, () -> PlayerProfileStorage.load(file));
    }
}
