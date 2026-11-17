package dev.aether.client.game;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.CRC32;

/** Checksummed atomic storage for local player position and inventory state. */
public final class PlayerProfileStorage {
    private static final int MAGIC = 0x41455046;
    private static final int VERSION = 1;
    private static final int HEADER_BYTES = 8;
    private static final int POSITION_BYTES = 3 * Double.BYTES;
    private static final int HOTBAR_BYTES = Integer.BYTES;
    private static final int SLOT_COUNT_BYTES = Integer.BYTES;
    private static final int STACK_BYTES = 2 * Integer.BYTES;
    private static final int CHECKSUM_BYTES = Long.BYTES;
    private static final int FILE_BYTES = HEADER_BYTES + POSITION_BYTES + HOTBAR_BYTES
            + SLOT_COUNT_BYTES + PlayerInventory.SLOT_COUNT * STACK_BYTES + CHECKSUM_BYTES;

    private PlayerProfileStorage() {}

    public static Optional<PlayerProfile> load(Path source) throws IOException {
        Objects.requireNonNull(source, "source");
        if (Files.notExists(source)) return Optional.empty();
        if (Files.size(source) != FILE_BYTES) throw new IOException("Invalid player profile size");
        byte[] file = Files.readAllBytes(source);
        int payloadLength = file.length - CHECKSUM_BYTES;
        CRC32 crc = new CRC32();
        crc.update(file, 0, payloadLength);
        long storedChecksum;
        try (DataInputStream checksum = new DataInputStream(
                new ByteArrayInputStream(file, payloadLength, CHECKSUM_BYTES))) {
            storedChecksum = checksum.readLong();
        }
        if (storedChecksum != crc.getValue()) throw new IOException("Player profile checksum mismatch");

        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(file, 0, payloadLength))) {
            if (input.readInt() != MAGIC) throw new IOException("Invalid player profile signature");
            if (input.readInt() != VERSION) throw new IOException("Unsupported player profile version");
            PlayerState player;
            try {
                player = new PlayerState(input.readDouble(), input.readDouble(), input.readDouble());
            } catch (IllegalArgumentException invalidPosition) {
                throw new IOException("Invalid player position in profile", invalidPosition);
            }
            int selected = input.readInt();
            if (selected < 0 || selected >= PlayerInventory.HOTBAR_SIZE) {
                throw new IOException("Invalid selected hotbar slot");
            }
            if (input.readInt() != PlayerInventory.SLOT_COUNT) throw new IOException("Invalid inventory slot count");
            PlayerInventory inventory = new PlayerInventory();
            inventory.selectHotbarSlot(selected);
            for (int slot = 0; slot < PlayerInventory.SLOT_COUNT; slot++) {
                int itemId = input.readInt();
                int count = input.readInt();
                try {
                    inventory.setSlot(slot, new ItemStack(itemId, count));
                } catch (IllegalArgumentException invalidStack) {
                    throw new IOException("Invalid item stack at slot " + slot, invalidStack);
                }
            }
            if (input.available() != 0) throw new IOException("Trailing data in player profile");
            return Optional.of(new PlayerProfile(player, inventory));
        }
    }

    public static void save(Path destination, PlayerState player, PlayerInventory inventory) throws IOException {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(inventory, "inventory");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(FILE_BYTES - CHECKSUM_BYTES);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeDouble(player.x());
            output.writeDouble(player.y());
            output.writeDouble(player.z());
            output.writeInt(inventory.selectedHotbarSlot());
            output.writeInt(PlayerInventory.SLOT_COUNT);
            for (int slot = 0; slot < PlayerInventory.SLOT_COUNT; slot++) {
                ItemStack stack = inventory.slot(slot);
                output.writeInt(stack.itemId());
                output.writeInt(stack.count());
            }
        }
        byte[] payload = bytes.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(payload);
        Path absolute = destination.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, absolute.getFileName().toString(), ".tmp");
        try {
            try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(temporary))) {
                output.write(payload);
                output.writeLong(crc.getValue());
            }
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public record PlayerProfile(PlayerState player, PlayerInventory inventory) {
        public PlayerProfile {
            Objects.requireNonNull(player, "player");
            Objects.requireNonNull(inventory, "inventory");
        }
    }
}
