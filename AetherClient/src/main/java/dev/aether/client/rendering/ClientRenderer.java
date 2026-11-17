package dev.aether.client.rendering;

import dev.aether.client.game.BlockRaycaster;

import java.util.Optional;

/** Backend-neutral rendering and picking contract used by the client loop. */
public interface ClientRenderer extends AutoCloseable {
    void initialize();
    void render(int framebufferWidth, int framebufferHeight, float deltaSeconds);
    void setPlayerPosition(double x, double y, double z);
    void setHotbarState(int selectedSlot, boolean[] occupied);
    void setInventoryState(int[] itemIds, int[] counts, int selectedSlot, boolean open,
                           int[] craftingItemIds, int[] craftingCounts, int resultItemId,
                           int resultCount, int selectedCraftSlot);
    void setKeyBindingEditorState(String[] actions, String[] bindings, int selectedRow,
                                  boolean open, boolean capturing);
    String storageError();
    Optional<BlockRaycaster.Hit> pickBlock(double cursorX, double cursorY, int windowWidth, int windowHeight);
    void moveCamera(float deltaX, float deltaY);
    void changeZoom(float delta);
    @Override void close();
}
