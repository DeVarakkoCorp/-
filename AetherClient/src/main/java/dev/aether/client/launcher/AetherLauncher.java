package dev.aether.client.launcher;

import dev.aether.client.platform.GlfwWindow;
import dev.aether.client.core.FixedStepClock;
import dev.aether.client.world.chunk.ChunkWorld;
import dev.aether.client.world.chunk.ChunkWorldStorage;
import dev.aether.client.world.chunk.DemoWorldGenerator;
import dev.aether.client.world.chunk.ChunkStore;
import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.RenderLayer;
import dev.aether.client.game.PlayerInput;
import dev.aether.client.game.BlockInteraction;
import dev.aether.client.game.PlayerPhysics;
import dev.aether.client.game.PlayerState;
import dev.aether.client.game.InputBindings;
import dev.aether.client.game.InputBindingStorage;
import dev.aether.client.game.InputAction;
import dev.aether.client.game.PlayerInventory;
import dev.aether.client.game.CraftingGrid;
import dev.aether.client.game.ItemStack;
import dev.aether.client.game.ItemNames;
import dev.aether.client.game.KeyBindingEditor;
import dev.aether.client.game.GameKey;
import dev.aether.client.game.MouseButton;
import dev.aether.client.game.PlayerProfileStorage;
import dev.aether.client.game.BlockBreakingController;
import dev.aether.client.rendering.ClientRenderer;
import dev.aether.client.rendering.RendererFactory;
import dev.aether.client.rendering.opengl.OpenGLRendererFactory;
import dev.aether.client.rendering.opengl.OpenGLCapabilities;
import dev.aether.client.rendering.InventoryLayout;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public final class AetherLauncher {
    private static final InputAction[] HOTBAR_ACTIONS = {
            InputAction.HOTBAR_1, InputAction.HOTBAR_2, InputAction.HOTBAR_3,
            InputAction.HOTBAR_4, InputAction.HOTBAR_5, InputAction.HOTBAR_6,
            InputAction.HOTBAR_7, InputAction.HOTBAR_8, InputAction.HOTBAR_9
    };

    private AetherLauncher() {}

    public static void main(String[] args) throws java.io.IOException {
        GLFWErrorCallback errorCallback = GLFWErrorCallback.createPrint(System.err);
        errorCallback.set();

        GlfwWindow window = null;
        ClientRenderer renderer = null;
        ChunkWorld world = null;
        Path worldFile = Path.of("saves", "default", "world.aether");
        Path playerFile = Path.of("saves", "default", "player.aether");
        PlayerState player = null;
        PlayerInventory inventory = null;
        CraftingGrid craftingGrid = new CraftingGrid();
        int selectedCraftSlot = -1;
        InputBindings inputBindings = null;
        Path keybindingsFile = Path.of("saves", "default", "keybindings.properties");
        boolean inventoryOpen = false;
        boolean keyBindingEditorOpen = false;
        KeyBindingEditor keyBindingEditor = new KeyBindingEditor();
        boolean glfwInitialized = false;

        try {
            if (!glfwInit()) {
                throw new IllegalStateException("GLFW initialization failed");
            }
            glfwInitialized = true;

            window = new GlfwWindow(1280, 720, "Aether Client");
            window.create();

            glfwMakeContextCurrent(window.handle());
            glfwSwapInterval(1);
            GL.createCapabilities();
            OpenGLCapabilities capabilities = OpenGLCapabilities.query();
            System.out.printf("OpenGL %d.%d (%s) — %s / %s; max texture %d, color targets %d, samples %d%n",
                    capabilities.majorVersion(), capabilities.minorVersion(), capabilities.version(),
                    capabilities.vendor(), capabilities.renderer(), capabilities.maxTextureSize(),
                    capabilities.maxColorAttachments(), capabilities.maxSamples());

            world = Files.exists(worldFile) ? ChunkWorldStorage.load(worldFile) : DemoWorldGenerator.create(1);
            RendererFactory rendererFactory = new OpenGLRendererFactory();
            renderer = rendererFactory.create(world, new ChunkStore(Path.of("saves", "default", "chunks")));
            renderer.initialize();

            BlockRegistry blockRegistry = BlockRegistry.builder()
                    .register(new BlockDefinition(1, RenderLayer.OPAQUE, true, true, 0, 0.5))
                    .register(new BlockDefinition(2, RenderLayer.OPAQUE, true, true, 0, 1.5))
                    .build();
            BlockInteraction blockInteraction = new BlockInteraction();
            PlayerPhysics physics = new PlayerPhysics(blockRegistry);
            BlockBreakingController breaking = new BlockBreakingController();
            inputBindings = InputBindingStorage.load(keybindingsFile);
            var savedProfile = PlayerProfileStorage.load(playerFile);
            if (savedProfile.isPresent()) {
                player = savedProfile.get().player();
                inventory = savedProfile.get().inventory();
            } else {
                player = new PlayerState(8.0, 8.0, 8.0);
                inventory = new PlayerInventory();
                inventory.setSlot(0, new ItemStack(1, 64));
            }
            renderer.setPlayerPosition(player.x(), player.y(), player.z());
            FixedStepClock simulationClock = new FixedStepClock(0.05, 5);
            long previousNanos = System.nanoTime();
            String lastWindowTitle = "";
            while (!window.shouldClose()) {
                glfwPollEvents();
                long now = System.nanoTime();
                double frameSeconds = Math.max(0.0, (now - previousNanos) / 1_000_000_000.0);
                previousNanos = now;

                if (!keyBindingEditorOpen && window.consumeKeyPress(GameKey.F1)) {
                    keyBindingEditorOpen = true;
                    inventoryOpen = false;
                }
                if (keyBindingEditorOpen) {
                    if (keyBindingEditor.capturing()) {
                        GameKey pressedKey = window.consumeAnyKeyPress();
                        MouseButton pressedMouse = window.consumeAnyMousePress();
                        if (pressedKey != null || pressedMouse != null) {
                            boolean changed = keyBindingEditor.capture(pressedKey, pressedMouse, inputBindings);
                            if (changed) {
                                try {
                                    InputBindingStorage.save(keybindingsFile, inputBindings);
                                } catch (java.io.IOException saveError) {
                                    keyBindingEditor.reportSaveFailure(saveError.getMessage());
                                }
                            }
                        }
                    } else if (window.consumeKeyPress(GameKey.ESCAPE) || window.consumeKeyPress(GameKey.F1)) {
                        keyBindingEditorOpen = false;
                    } else if (window.consumeKeyPress(GameKey.UP)) {
                        keyBindingEditor.moveSelection(-1);
                    } else if (window.consumeKeyPress(GameKey.DOWN)) {
                        keyBindingEditor.moveSelection(1);
                    } else if (window.consumeKeyPress(GameKey.ENTER)) {
                        keyBindingEditor.beginCapture();
                    }
                    while (window.consumeAnyKeyPress() != null) { /* discard unrelated menu input */ }
                    while (window.consumeAnyMousePress() != null) { /* do not click through the menu */ }
                }
                window.setEscapeClosesWindow(!keyBindingEditorOpen);

                if (!keyBindingEditorOpen && inputBindings.consumePress(InputAction.TOGGLE_INVENTORY, window)) {
                    if (!inventoryOpen) inventoryOpen = true;
                    else if (craftingGrid.returnItems(inventory)) {
                        inventoryOpen = false;
                        selectedCraftSlot = -1;
                    }
                }

                if (keyBindingEditorOpen) {
                    window.consumeMousePress(MouseButton.LEFT);
                    window.consumeMousePress(MouseButton.RIGHT);
                    inputBindings.consumePress(InputAction.MINE, window);
                    inputBindings.consumePress(InputAction.PLACE, window);
                } else if (inventoryOpen) {
                    if (window.consumeMousePress(MouseButton.LEFT)) {
                        double pixelX = window.cursorX() * window.framebufferWidth() / window.windowWidth();
                        double pixelY = window.cursorY() * window.framebufferHeight() / window.windowHeight();
                        int clickedSlot = InventoryLayout.slotAt(pixelX, pixelY,
                                window.framebufferWidth(), window.framebufferHeight());
                        int clickedCraftCell = InventoryLayout.craftingSlotAt(pixelX, pixelY,
                                window.framebufferWidth(), window.framebufferHeight());
                        if (clickedCraftCell >= 0) {
                            selectedCraftSlot = clickedCraftCell;
                        } else if (InventoryLayout.craftingOutputAt(pixelX, pixelY,
                                window.framebufferWidth(), window.framebufferHeight())) {
                            craftingGrid.craft(inventory);
                        } else if (clickedSlot >= 0 && selectedCraftSlot >= 0) {
                            craftingGrid.addOne(inventory, clickedSlot, selectedCraftSlot);
                        } else if (clickedSlot >= 0 && clickedSlot < PlayerInventory.HOTBAR_SIZE) {
                            inventory.selectHotbarSlot(clickedSlot);
                        } else if (clickedSlot >= PlayerInventory.HOTBAR_SIZE) {
                            inventory.swapSlots(clickedSlot, inventory.selectedHotbarSlot());
                        }
                    }
                    window.consumeMousePress(MouseButton.RIGHT);
                    inputBindings.consumePress(InputAction.MINE, window);
                    inputBindings.consumePress(InputAction.PLACE, window);
                } else {
                    for (int slot = 0; slot < HOTBAR_ACTIONS.length; slot++) {
                        if (inputBindings.consumePress(HOTBAR_ACTIONS[slot], window)) {
                            inventory.selectHotbarSlot(slot);
                        }
                    }
                }

                if (!keyBindingEditorOpen && !inventoryOpen && inputBindings.isDown(InputAction.MINE, window)) {
                    var hit = renderer.pickBlock(window.cursorX(), window.cursorY(),
                            window.windowWidth(), window.windowHeight());
                    if (hit.isPresent()) {
                        double miningSeconds = blockRegistry.get(hit.get().blockId()).miningSeconds();
                        if (breaking.advance(hit.get(), frameSeconds, miningSeconds)) {
                            blockInteraction.mine(world, blockRegistry, hit.get(), inventory);
                            breaking.reset();
                        }
                    } else {
                        breaking.reset();
                    }
                } else {
                    breaking.reset();
                }
                inputBindings.consumePress(InputAction.MINE, window);
                if (!keyBindingEditorOpen && !inventoryOpen && inputBindings.consumePress(InputAction.PLACE, window)) {
                    var hit = renderer.pickBlock(window.cursorX(), window.cursorY(),
                            window.windowWidth(), window.windowHeight());
                    if (hit.isPresent()) blockInteraction.place(world, blockRegistry, hit.get(), inventory, player);
                }

                ItemStack selectedStack = inventory.selectedStack();
                String itemName = ItemNames.name(selectedStack.itemId());
                String hoveredItem = "";
                if (inventoryOpen) {
                    double pixelX = window.cursorX() * window.framebufferWidth() / window.windowWidth();
                    double pixelY = window.cursorY() * window.framebufferHeight() / window.windowHeight();
                    int hoveredSlot = InventoryLayout.slotAt(pixelX, pixelY,
                            window.framebufferWidth(), window.framebufferHeight());
                    int hoveredCraftSlot = InventoryLayout.craftingSlotAt(pixelX, pixelY,
                            window.framebufferWidth(), window.framebufferHeight());
                    int hoveredItemId = hoveredSlot >= 0 ? inventory.slot(hoveredSlot).itemId()
                            : hoveredCraftSlot >= 0 ? craftingGrid.input(hoveredCraftSlot).itemId()
                            : InventoryLayout.craftingOutputAt(pixelX, pixelY,
                                    window.framebufferWidth(), window.framebufferHeight())
                                    ? craftingGrid.result().itemId() : 0;
                    if (hoveredItemId != 0) hoveredItem = " | Hover: " + ItemNames.name(hoveredItemId);
                }
                String windowTitle = "Aether Client | Slot " + (inventory.selectedHotbarSlot() + 1)
                        + ": " + itemName + " x" + selectedStack.count()
                        + hoveredItem
                        + (keyBindingEditorOpen ? " | " + keyBindingEditor.selectedAction().name()
                                + " → " + bindingLabel(inputBindings, keyBindingEditor.selectedAction())
                                + " | " + keyBindingEditor.message()
                                + (keyBindingEditor.capturing() ? " | Press a key or mouse button" : " | F1/Esc close") : "")
                        + (inventoryOpen ? " | Craft: place 1 Stone in each input cell; click result; E to close" : "")
                        + (breaking.progress() > 0.0
                        ? " | Breaking " + Math.round(breaking.progress() * 100.0) + "%" : "")
                        + (renderer.storageError().isEmpty() ? "" : " | Storage: " + renderer.storageError());
                if (!windowTitle.equals(lastWindowTitle)) {
                    window.setTitle(windowTitle);
                    lastWindowTitle = windowTitle;
                }
                renderer.setHotbarState(inventory.selectedHotbarSlot(), inventory.hotbarOccupancy());
                int[] inventoryItemIds = new int[PlayerInventory.SLOT_COUNT];
                int[] inventoryCounts = new int[PlayerInventory.SLOT_COUNT];
                for (int slot = 0; slot < PlayerInventory.SLOT_COUNT; slot++) {
                    ItemStack stack = inventory.slot(slot);
                    inventoryItemIds[slot] = stack.itemId();
                    inventoryCounts[slot] = stack.count();
                }
                renderer.setInventoryState(inventoryItemIds, inventoryCounts,
                        inventory.selectedHotbarSlot(), inventoryOpen, craftingGrid.itemIds(),
                        craftingGrid.counts(), craftingGrid.result().itemId(), craftingGrid.result().count(),
                        selectedCraftSlot);
                String[] editorActions = new String[InputAction.values().length];
                String[] editorBindings = new String[editorActions.length];
                for (int index = 0; index < editorActions.length; index++) {
                    InputAction action = InputAction.values()[index];
                    editorActions[index] = action.name().replace('_', ' ');
                    editorBindings[index] = bindingLabel(inputBindings, action);
                }
                renderer.setKeyBindingEditorState(editorActions, editorBindings,
                        keyBindingEditor.selectedIndex(), keyBindingEditorOpen, keyBindingEditor.capturing());

                FixedStepClock.Frame simulationFrame = simulationClock.advance(frameSeconds);
                for (int step = 0; step < simulationFrame.steps(); step++) {
                    float panSpeed = 0.8f * (float) simulationClock.stepSeconds();
                    if (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.CAMERA_LEFT, window)) renderer.moveCamera(panSpeed, 0.0f);
                    if (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.CAMERA_RIGHT, window)) renderer.moveCamera(-panSpeed, 0.0f);
                    if (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.CAMERA_UP, window)) renderer.moveCamera(0.0f, -panSpeed);
                    if (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.CAMERA_DOWN, window)) renderer.moveCamera(0.0f, panSpeed);
                    if (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.ZOOM_IN, window)) renderer.changeZoom(panSpeed);
                    if (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.ZOOM_OUT, window)) renderer.changeZoom(-panSpeed);

                    double oldX = player.x();
                    double oldZ = player.z();
                    PlayerInput input = new PlayerInput(
                            (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.MOVE_RIGHT, window) ? 1.0f : 0.0f)
                                    - (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.MOVE_LEFT, window) ? 1.0f : 0.0f),
                            (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.MOVE_FORWARD, window) ? 1.0f : 0.0f)
                                    - (!inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.MOVE_BACKWARD, window) ? 1.0f : 0.0f),
                            !inventoryOpen && !keyBindingEditorOpen && inputBindings.isDown(InputAction.JUMP, window));
                    physics.tick(player, input, world, simulationClock.stepSeconds());
                    renderer.setPlayerPosition(player.x(), player.y(), player.z());
                    float followX = (float) (-(player.x() - oldX - (player.z() - oldZ)) * 0.04);
                    float followY = (float) (-(player.x() - oldX + (player.z() - oldZ)) * 0.02);
                    renderer.moveCamera(followX, followY);
                }

                int width = window.framebufferWidth();
                int height = window.framebufferHeight();
                if (width > 0 && height > 0) {
                    renderer.render(width, height,
                            (float) (simulationFrame.interpolation() * simulationClock.stepSeconds()));
                    glfwSwapBuffers(window.handle());
                }
            }
        } finally {
            try {
                if (renderer != null) renderer.close();
            } finally {
                try {
                    if (window != null) window.close();
                } finally {
                    try {
                        if (glfwInitialized) glfwTerminate();
                    } finally {
                        errorCallback.free();
                        if (world != null) ChunkWorldStorage.save(world, worldFile);
                        if (player != null && inventory != null) {
                            craftingGrid.returnItems(inventory);
                            PlayerProfileStorage.save(playerFile, player, inventory);
                        }
                        if (inputBindings != null) InputBindingStorage.save(keybindingsFile, inputBindings);
                    }
                }
            }
        }
    }

    private static String bindingLabel(InputBindings bindings, InputAction action) {
        GameKey key = bindings.keyFor(action);
        return key == null ? "MOUSE " + bindings.mouseButtonFor(action).name() : "KEY " + key.name();
    }
}
