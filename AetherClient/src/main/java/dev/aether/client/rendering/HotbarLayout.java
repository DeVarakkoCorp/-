package dev.aether.client.rendering;

import java.util.Objects;

/** Builds simple pixel-aligned hotbar rectangles as position/color triangle vertices. */
public final class HotbarLayout {
    public static final int SLOT_COUNT = 9;
    private static final int SLOT_SIZE = 40;
    private static final int GAP = 4;
    private static final int BOTTOM_MARGIN = 18;
    private static final int PADDING = 8;

    private HotbarLayout() {}

    public static float[] vertices(int width, int height, int selectedSlot, boolean[] occupied) {
        Objects.requireNonNull(occupied, "occupied");
        if (width <= 0 || height <= 0) throw new IllegalArgumentException("Viewport must be positive");
        if (selectedSlot < 0 || selectedSlot >= SLOT_COUNT) throw new IllegalArgumentException("Selected slot must be 0..8");
        if (occupied.length != SLOT_COUNT) throw new IllegalArgumentException("Hotbar occupancy must contain nine slots");

        FloatBuilder vertices = new FloatBuilder();
        int totalWidth = SLOT_COUNT * SLOT_SIZE + (SLOT_COUNT - 1) * GAP;
        int left = (width - totalWidth) / 2;
        int top = height - BOTTOM_MARGIN - SLOT_SIZE;
        rectangle(vertices, width, height, left - PADDING, top - PADDING,
                left + totalWidth + PADDING, top + SLOT_SIZE + PADDING, 0.035f, 0.045f, 0.075f);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            int slotLeft = left + slot * (SLOT_SIZE + GAP);
            float border = slot == selectedSlot ? 0.98f : 0.42f;
            rectangle(vertices, width, height, slotLeft, top, slotLeft + SLOT_SIZE, top + SLOT_SIZE,
                    border, slot == selectedSlot ? 0.78f : border, 0.24f);
            rectangle(vertices, width, height, slotLeft + 3, top + 3, slotLeft + SLOT_SIZE - 3,
                    top + SLOT_SIZE - 3, 0.10f, 0.12f, 0.16f);
            if (occupied[slot]) {
                rectangle(vertices, width, height, slotLeft + 11, top + 11,
                        slotLeft + SLOT_SIZE - 11, top + SLOT_SIZE - 11, 0.20f, 0.66f, 0.34f);
            }
        }
        return vertices.toArray();
    }

    private static void rectangle(FloatBuilder out, int width, int height,
                                  int left, int top, int right, int bottom,
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
        private float[] values = new float[600];
        private int size;

        void vertex(float x, float y, float red, float green, float blue) {
            ensureCapacity(5);
            values[size++] = x;
            values[size++] = y;
            values[size++] = red;
            values[size++] = green;
            values[size++] = blue;
        }

        float[] toArray() { return java.util.Arrays.copyOf(values, size); }

        private void ensureCapacity(int additional) {
            if (size + additional <= values.length) return;
            values = java.util.Arrays.copyOf(values, Math.max(values.length * 2, size + additional));
        }
    }
}
