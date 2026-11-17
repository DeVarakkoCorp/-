package dev.aether.client.rendering;

import java.util.Objects;

/** Pixel-aligned 9×4 inventory grid. Slots 0–8 form the bottom hotbar row. */
public final class InventoryLayout {
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int SLOT = 42;
    private static final int GAP = 6;
    private static final int PANEL_PADDING = 24;
    private static final int CRAFTING_HEIGHT = 108;
    private static final String[] DIGITS = {
            "111101101101111", "010110010010111", "111001111100111", "111001111001111",
            "101101111001001", "111100111001111", "111100111101111", "111001001001001",
            "111101111101111", "111101111001111"
    };

    private InventoryLayout() {}

    public static float[] vertices(int width, int height, int selectedSlot, int[] itemIds, int[] counts) {
        return vertices(width, height, selectedSlot, itemIds, counts, new int[4], new int[4], 0, 0, -1);
    }

    public static float[] vertices(int width, int height, int selectedSlot, int[] itemIds, int[] counts,
                                   int[] craftingItemIds, int[] craftingCounts, int resultItemId,
                                   int resultCount, int selectedCraftSlot) {
        validate(width, height, selectedSlot, itemIds, counts);
        validateStacks(craftingItemIds, craftingCounts, 4);
        if (resultItemId < 0 || resultCount < 0 || resultCount > 64
                || (resultCount == 0) != (resultItemId == 0)) {
            throw new IllegalArgumentException("Invalid crafting result");
        }
        if (selectedCraftSlot < -1 || selectedCraftSlot >= 4) {
            throw new IllegalArgumentException("Selected crafting slot must be -1..3");
        }
        FloatBuilder out = new FloatBuilder();
        int gridWidth = COLUMNS * SLOT + (COLUMNS - 1) * GAP;
        int gridHeight = ROWS * SLOT + (ROWS - 1) * GAP;
        int left = (width - gridWidth) / 2;
        int top = (height - gridHeight - CRAFTING_HEIGHT) / 2;
        rectangle(out, width, height, left - PANEL_PADDING, top - PANEL_PADDING,
                left + gridWidth + PANEL_PADDING, top + CRAFTING_HEIGHT + gridHeight + PANEL_PADDING,
                0.035f, 0.045f, 0.075f);

        for (int slot = 0; slot < 4; slot++) {
            int x = craftingX(left, slot);
            int y = craftingY(top, slot);
            drawSlot(out, width, height, x, y, craftingItemIds[slot], craftingCounts[slot],
                    selectedCraftSlot == slot);
        }
        int outputX = left + 170;
        int outputY = top + 16;
        drawSlot(out, width, height, outputX, outputY, resultItemId, resultCount, false);

        for (int row = 0; row < ROWS; row++) {
            for (int column = 0; column < COLUMNS; column++) {
                int slot = row == ROWS - 1 ? column : (row * COLUMNS + column + COLUMNS);
                int x = left + column * (SLOT + GAP);
                int y = top + CRAFTING_HEIGHT + row * (SLOT + GAP);
                boolean selected = slot == selectedSlot;
                drawSlot(out, width, height, x, y, itemIds[slot], counts[slot], selected);
            }
        }
        return out.toArray();
    }

    private static void drawCount(FloatBuilder out, int width, int height, int count, int slotX, int slotY) {
        String text = Integer.toString(count);
        int pixel = 2;
        int textWidth = text.length() * 3 * pixel + (text.length() - 1) * pixel;
        int originX = slotX + SLOT - 5 - textWidth;
        int originY = slotY + SLOT - 5 - 5 * pixel;
        for (int index = 0; index < text.length(); index++) {
            String glyph = DIGITS[text.charAt(index) - '0'];
            for (int bit = 0; bit < glyph.length(); bit++) {
                if (glyph.charAt(bit) != '1') continue;
                int x = originX + index * 4 * pixel + bit % 3 * pixel;
                int y = originY + bit / 3 * pixel;
                rectangle(out, width, height, x, y, x + pixel, y + pixel, 0.98f, 0.98f, 1.0f);
            }
        }
    }

    private static void drawSlot(FloatBuilder out, int width, int height, int x, int y,
                                 int itemId, int count, boolean selected) {
        float border = selected ? 1.0f : 0.42f;
        rectangle(out, width, height, x, y, x + SLOT, y + SLOT,
                border, selected ? 0.78f : border, 0.24f);
        rectangle(out, width, height, x + 3, y + 3, x + SLOT - 3, y + SLOT - 3,
                0.10f, 0.12f, 0.16f);
        if (count > 0) {
            float[] color = itemColor(itemId);
            rectangle(out, width, height, x + 10, y + 8, x + SLOT - 10, y + SLOT - 13,
                    color[0], color[1], color[2]);
            int barRight = x + 8 + Math.max(3, (SLOT - 16) * count / 64);
            rectangle(out, width, height, x + 8, y + SLOT - 8, barRight, y + SLOT - 5,
                    0.90f, 0.92f, 0.96f);
            drawCount(out, width, height, count, x, y);
        }
    }

    /** Returns an inventory slot, or -1 when the cursor is outside the grid. */
    public static int slotAt(double cursorX, double cursorY, int width, int height) {
        if (width <= 0 || height <= 0 || !Double.isFinite(cursorX) || !Double.isFinite(cursorY)) return -1;
        int gridWidth = COLUMNS * SLOT + (COLUMNS - 1) * GAP;
        int gridHeight = ROWS * SLOT + (ROWS - 1) * GAP;
        int left = (width - gridWidth) / 2;
        int top = (height - gridHeight - CRAFTING_HEIGHT) / 2 + CRAFTING_HEIGHT;
        int x = (int) Math.floor(cursorX) - left;
        int y = (int) Math.floor(cursorY) - top;
        if (x < 0 || y < 0 || x >= gridWidth || y >= gridHeight) return -1;
        int column = x / (SLOT + GAP);
        int row = y / (SLOT + GAP);
        if (column >= COLUMNS || row >= ROWS || x % (SLOT + GAP) >= SLOT || y % (SLOT + GAP) >= SLOT) return -1;
        return row == ROWS - 1 ? column : row * COLUMNS + column + COLUMNS;
    }

    /** Returns a 2×2 crafting input slot or -1. */
    public static int craftingSlotAt(double cursorX, double cursorY, int width, int height) {
        if (!finiteCursor(cursorX, cursorY, width, height)) return -1;
        int gridWidth = COLUMNS * SLOT + (COLUMNS - 1) * GAP;
        int left = (width - gridWidth) / 2;
        int top = (height - (ROWS * SLOT + (ROWS - 1) * GAP) - CRAFTING_HEIGHT) / 2;
        for (int slot = 0; slot < 4; slot++) {
            if (inside(cursorX, cursorY, craftingX(left, slot), craftingY(top, slot))) return slot;
        }
        return -1;
    }

    public static boolean craftingOutputAt(double cursorX, double cursorY, int width, int height) {
        if (!finiteCursor(cursorX, cursorY, width, height)) return false;
        int gridWidth = COLUMNS * SLOT + (COLUMNS - 1) * GAP;
        int left = (width - gridWidth) / 2;
        int top = (height - (ROWS * SLOT + (ROWS - 1) * GAP) - CRAFTING_HEIGHT) / 2;
        return inside(cursorX, cursorY, left + 170, top + 16);
    }

    private static void validate(int width, int height, int selectedSlot, int[] itemIds, int[] counts) {
        validateStacks(itemIds, counts, 36);
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Viewport must be positive");
        if (selectedSlot < 0 || selectedSlot >= 9) throw new IllegalArgumentException("Selected slot must be 0..8");
    }

    private static void validateStacks(int[] itemIds, int[] counts, int expectedLength) {
        Objects.requireNonNull(itemIds, "itemIds");
        Objects.requireNonNull(counts, "counts");
        if (itemIds.length != expectedLength || counts.length != expectedLength) {
            throw new IllegalArgumentException("Expected " + expectedLength + " item ids and counts");
        }
        for (int i = 0; i < expectedLength; i++) {
            if (itemIds[i] < 0 || counts[i] < 0 || counts[i] > 64 || (counts[i] == 0) != (itemIds[i] == 0)) {
                throw new IllegalArgumentException("Invalid item stack at slot " + i);
            }
        }
    }

    private static int craftingX(int left, int slot) { return left + 12 + (slot % 2) * 48; }
    private static int craftingY(int top, int slot) { return top + 5 + (slot / 2) * 48; }

    private static boolean finiteCursor(double x, double y, int width, int height) {
        return width > 0 && height > 0 && Double.isFinite(x) && Double.isFinite(y);
    }

    private static boolean inside(double x, double y, int left, int top) {
        return x >= left && x < left + SLOT && y >= top && y < top + SLOT;
    }

    private static float[] itemColor(int itemId) {
        return switch (itemId) {
            case 1 -> new float[]{0.56f, 0.59f, 0.64f};
            case 2 -> new float[]{0.46f, 0.28f, 0.16f};
            default -> new float[]{0.25f + (itemId * 37 % 60) / 100.0f,
                    0.25f + (itemId * 17 % 60) / 100.0f, 0.25f + (itemId * 29 % 60) / 100.0f};
        };
    }

    private static void rectangle(FloatBuilder out, int width, int height, int left, int top, int right, int bottom,
                                  float red, float green, float blue) {
        float x1 = left * 2.0f / width - 1.0f;
        float x2 = right * 2.0f / width - 1.0f;
        float y1 = 1.0f - top * 2.0f / height;
        float y2 = 1.0f - bottom * 2.0f / height;
        out.vertex(x1, y1, red, green, blue);
        out.vertex(x2, y1, red, green, blue);
        out.vertex(x2, y2, red, green, blue);
        out.vertex(x1, y1, red, green, blue);
        out.vertex(x2, y2, red, green, blue);
        out.vertex(x1, y2, red, green, blue);
    }

    private static final class FloatBuilder {
        private float[] values = new float[1800];
        private int size;

        void vertex(float x, float y, float red, float green, float blue) {
            if (size + 5 > values.length) values = java.util.Arrays.copyOf(values, values.length * 2);
            values[size++] = x;
            values[size++] = y;
            values[size++] = red;
            values[size++] = green;
            values[size++] = blue;
        }

        float[] toArray() { return java.util.Arrays.copyOf(values, size); }
    }
}
