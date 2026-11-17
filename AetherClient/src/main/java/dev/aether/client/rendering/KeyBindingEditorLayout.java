package dev.aether.client.rendering;

import java.util.Arrays;
import java.util.Map;

/** Pixel-aligned, readable action and binding list for the OpenGL controls screen. */
public final class KeyBindingEditorLayout {
    private static final int ROW_HEIGHT = 22;
    private static final int SCALE = 2;
    private static final Map<Character, String> FONT = Map.ofEntries(
            Map.entry('A', "010101101111101"), Map.entry('B', "110101110101110"),
            Map.entry('C', "011100100100011"), Map.entry('D', "110101101101110"),
            Map.entry('E', "111100110100111"), Map.entry('F', "111100110100100"),
            Map.entry('G', "011100101101011"), Map.entry('H', "101101111101101"),
            Map.entry('I', "111010010010111"), Map.entry('J', "001001001101010"),
            Map.entry('K', "101101110101101"), Map.entry('L', "100100100100111"),
            Map.entry('M', "101111111101101"), Map.entry('N', "110101101101101"),
            Map.entry('O', "010101101101010"), Map.entry('P', "110101110100100"),
            Map.entry('Q', "010101101111011"), Map.entry('R', "110101110101101"),
            Map.entry('S', "011100010001110"), Map.entry('T', "111010010010010"),
            Map.entry('U', "101101101101111"), Map.entry('V', "101101101101010"),
            Map.entry('W', "101101111111101"), Map.entry('X', "101101010101101"),
            Map.entry('Y', "101101010010010"), Map.entry('Z', "111001010100111"),
            Map.entry('0', "111101101101111"), Map.entry('1', "010110010010111"),
            Map.entry('2', "110001010100111"), Map.entry('3', "110001010001110"),
            Map.entry('4', "101101111001001"), Map.entry('5', "111100110001110"),
            Map.entry('6', "011100110101010"), Map.entry('7', "111001010010010"),
            Map.entry('8', "010101010101010"), Map.entry('9', "010101011001110"),
            Map.entry('_', "000000000000111"), Map.entry('-', "000000111000000"),
            Map.entry(' ', "000000000000000")
    );

    private KeyBindingEditorLayout() {}

    public static float[] vertices(int width, int height, String[] actions, String[] bindings,
                                   int selectedRow, boolean capturing) {
        if (width <= 0 || height <= 0 || actions == null || bindings == null
                || actions.length == 0 || actions.length != bindings.length
                || selectedRow < 0 || selectedRow >= actions.length) {
            throw new IllegalArgumentException("Invalid keybinding editor layout state");
        }
        int panelWidth = Math.min(840, width - 40);
        int panelHeight = 54 + actions.length * ROW_HEIGHT + 30;
        if (panelWidth <= 0 || panelHeight > height - 24) {
            throw new IllegalArgumentException("Viewport is too small for the keybinding list");
        }
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        Builder out = new Builder();
        out.rect(width, height, left, top, left + panelWidth, top + panelHeight, 0.025f, 0.035f, 0.060f);
        out.text(width, height, "CONTROLS", left + 24, top + 16, 2, 0.95f, 0.82f, 0.25f);
        for (int row = 0; row < actions.length; row++) {
            int y = top + 46 + row * ROW_HEIGHT;
            if (row == selectedRow) {
                out.rect(width, height, left + 12, y - 3, left + panelWidth - 12, y + 17,
                        capturing ? 0.40f : 0.16f, capturing ? 0.14f : 0.30f, 0.36f);
            }
            out.text(width, height, actions[row], left + 24, y, SCALE, 0.88f, 0.90f, 0.94f);
            out.text(width, height, bindings[row], left + panelWidth - 190, y, SCALE,
                    0.62f, 0.86f, 0.96f);
        }
        int footY = top + 49 + actions.length * ROW_HEIGHT;
        out.text(width, height, capturing ? "PRESS KEY OR MOUSE" : "UP DOWN MOVE ENTER REBIND ESC CLOSE",
                left + 24, footY, 1, 0.88f, 0.78f, 0.44f);
        return out.array();
    }

    private static final class Builder {
        private float[] data = new float[4096];
        private int size;

        void text(int width, int height, String value, int x, int y, int scale,
                  float red, float green, float blue) {
            String upper = value.toUpperCase(java.util.Locale.ROOT);
            for (int charIndex = 0; charIndex < upper.length(); charIndex++) {
                String glyph = FONT.getOrDefault(upper.charAt(charIndex), FONT.get(' '));
                for (int bit = 0; bit < 15; bit++) {
                    if (glyph.charAt(bit) != '1') continue;
                    int left = x + charIndex * 4 * scale + bit % 3 * scale;
                    int top = y + bit / 3 * scale;
                    rect(width, height, left, top, left + scale, top + scale, red, green, blue);
                }
            }
        }

        void rect(int width, int height, int left, int top, int right, int bottom,
                  float red, float green, float blue) {
            float x1 = left * 2.0f / width - 1.0f;
            float x2 = right * 2.0f / width - 1.0f;
            float y1 = 1.0f - top * 2.0f / height;
            float y2 = 1.0f - bottom * 2.0f / height;
            vertex(x1, y1, red, green, blue); vertex(x2, y1, red, green, blue);
            vertex(x2, y2, red, green, blue); vertex(x1, y1, red, green, blue);
            vertex(x2, y2, red, green, blue); vertex(x1, y2, red, green, blue);
        }

        private void vertex(float x, float y, float r, float g, float b) {
            if (size + 5 > data.length) data = Arrays.copyOf(data, data.length * 2);
            data[size++] = x; data[size++] = y; data[size++] = r; data[size++] = g; data[size++] = b;
        }

        float[] array() { return Arrays.copyOf(data, size); }
    }
}
