package dev.aether.client.world.chunk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

public final class ChunkWorldStorage {
    private static final int MAGIC = 0x41455448;
    private static final int VERSION = 1;
    private static final int MAX_SECTIONS = 1024;
    private static final int HEADER_BYTES = 12;
    private static final int SECTION_BYTES = 12 + ChunkSection.VOLUME * Integer.BYTES;
    private static final int CHECKSUM_BYTES = Long.BYTES;

    private ChunkWorldStorage() {}

    public static void save(ChunkWorld world, Path destination) throws IOException {
        Map<ChunkPos, ChunkSection> sections = world.snapshotSections();
        if (sections.size() > MAX_SECTIONS) throw new IOException("World exceeds section limit");

        ByteArrayOutputStream bytes = new ByteArrayOutputStream(HEADER_BYTES + sections.size() * SECTION_BYTES);
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeInt(MAGIC);
            output.writeInt(VERSION);
            output.writeInt(sections.size());
            for (Map.Entry<ChunkPos, ChunkSection> entry : sections.entrySet()) {
                ChunkPos position = entry.getKey();
                output.writeInt(position.x());
                output.writeInt(position.y());
                output.writeInt(position.z());
                for (int id : entry.getValue().copyBlockIds()) {
                    if (id < 0) throw new IOException("Negative block id in world data");
                    output.writeInt(id);
                }
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

    public static ChunkWorld load(Path source) throws IOException {
        long size = Files.size(source);
        if (size < HEADER_BYTES + CHECKSUM_BYTES || size > HEADER_BYTES
                + (long) MAX_SECTIONS * SECTION_BYTES + CHECKSUM_BYTES) {
            throw new IOException("Invalid world file size");
        }
        byte[] file = Files.readAllBytes(source);
        int payloadLength = file.length - CHECKSUM_BYTES;
        CRC32 crc = new CRC32();
        crc.update(file, 0, payloadLength);
        long storedChecksum;
        try (DataInputStream checksumInput = new DataInputStream(
                new ByteArrayInputStream(file, payloadLength, CHECKSUM_BYTES))) {
            storedChecksum = checksumInput.readLong();
        }
        if (storedChecksum != crc.getValue()) throw new IOException("World checksum mismatch");

        ChunkWorld world = new ChunkWorld();
        Set<ChunkPos> positions = new HashSet<>();
        try (DataInputStream input = new DataInputStream(new ByteArrayInputStream(file, 0, payloadLength))) {
            if (input.readInt() != MAGIC) throw new IOException("Invalid world file signature");
            if (input.readInt() != VERSION) throw new IOException("Unsupported world format version");
            int count = input.readInt();
            if (count < 0 || count > MAX_SECTIONS || payloadLength != HEADER_BYTES + (long) count * SECTION_BYTES) {
                throw new IOException("Invalid section count or payload length");
            }
            for (int i = 0; i < count; i++) {
                ChunkPos position = new ChunkPos(input.readInt(), input.readInt(), input.readInt());
                if (!positions.add(position)) throw new IOException("Duplicate section: " + position);
                ChunkSection section = new ChunkSection();
                for (int y = 0; y < ChunkSection.EDGE; y++) {
                    for (int z = 0; z < ChunkSection.EDGE; z++) {
                        for (int x = 0; x < ChunkSection.EDGE; x++) {
                            int id = input.readInt();
                            if (id < 0) throw new IOException("Negative block id in world file");
                            if (id != 0) section.setBlockId(x, y, z, id);
                        }
                    }
                }
                world.putSection(position, section);
            }
        }
        return world;
    }
}
