package dev.aether.client.rendering.opengl;

import dev.aether.client.world.BlockDefinition;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.BlockLightEngine;
import dev.aether.client.game.BlockRaycaster;
import dev.aether.client.world.LightMap;
import dev.aether.client.world.RenderLayer;
import dev.aether.client.world.SkyLightEngine;
import dev.aether.client.rendering.Camera2D;
import dev.aether.client.rendering.ClientRenderer;
import dev.aether.client.world.chunk.ChunkVisibility;
import dev.aether.client.world.chunk.ChunkMesh;
import dev.aether.client.world.chunk.ChunkStreamingController;
import dev.aether.client.world.chunk.ChunkMesher;
import dev.aether.client.world.chunk.ChunkMeshingService;
import dev.aether.client.world.chunk.ChunkPos;
import dev.aether.client.world.chunk.ChunkSection;
import dev.aether.client.world.chunk.ChunkWorld;
import dev.aether.client.world.chunk.ChunkStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public final class OpenGLRenderer implements ClientRenderer {
    private static final int VIEW_DISTANCE_CHUNKS = 1;
    private final Thread ownerThread = Thread.currentThread();
    private final GlResourceRegistry resources = new GlResourceRegistry(ownerThread);

    private ShaderProgram shader;
    private final Map<ChunkPos, OpenGLChunkMesh> chunkMeshes = new HashMap<>();
    private ChunkMeshingService meshingService;
    private ChunkStreamingController streamingController;
    private final Map<ChunkPos, CompletableFuture<ChunkMeshingService.MeshResult>> pendingMeshes = new HashMap<>();
    private int cameraOffsetLocation;
    private int zoomLocation;
    private int modelOffsetLocation;
    private int modelScaleLocation;
    private int modelColorLocation;
    private final Camera2D camera = new Camera2D();
    private boolean initialized;
    private final ChunkWorld world;
    private final ChunkStore chunkStore;
    private BlockRegistry blockRegistry;
    private long preparedWorldRevision = -1;
    private boolean visibleRegionDirty = true;
    private int playerChunkX = Integer.MIN_VALUE;
    private int playerChunkZ = Integer.MIN_VALUE;
    private LightMap lightMap = LightMap.DARK;
    private OpenGLChunkMesh playerMesh;
    private OpenGLHud hud;
    private int selectedHotbarSlot;
    private boolean[] hotbarOccupied = new boolean[9];
    private int[] inventoryItemIds = new int[36];
    private int[] inventoryCounts = new int[36];
    private int[] craftingItemIds = new int[4];
    private int[] craftingCounts = new int[4];
    private int craftingResultItemId;
    private int craftingResultCount;
    private int selectedCraftSlot = -1;
    private boolean inventoryOpen;
    private String[] editorActions = new String[0];
    private String[] editorBindings = new String[0];
    private int selectedEditorRow;
    private boolean editorOpen;
    private boolean editorCapturing;
    private float playerX;
    private float playerY;
    private float playerZ;

    public OpenGLRenderer(ChunkWorld world) {
        this(world, null);
    }

    public OpenGLRenderer(ChunkWorld world, ChunkStore chunkStore) {
        this.world = java.util.Objects.requireNonNull(world, "world");
        this.chunkStore = chunkStore;
    }

    private static final String VERTEX_SHADER = """
            #version 330 core
            layout (location = 0) in vec3 position;
            layout (location = 1) in vec3 normal;
            layout (location = 2) in float blockLight;
            uniform vec2 cameraOffset;
            uniform float cameraZoom;
            uniform vec3 modelOffset;
            uniform vec3 modelScale;
            out float light;
            void main() {
                vec3 worldPosition = position * modelScale + modelOffset;
                vec3 centered = worldPosition - vec3(8.0, 0.0, 8.0);
                vec2 projected = vec2(centered.x - centered.z,
                                      (centered.x + centered.z) * 0.5 - worldPosition.y * 1.25);
                float depth = -(1.25 * centered.x + worldPosition.y + 1.25 * centered.z) * 0.005;
                gl_Position = vec4(projected * 0.04 * cameraZoom + cameraOffset, depth, 1.0);
                float directional = 0.45 + max(dot(normal, normalize(vec3(-0.4, 1.0, 0.3))), 0.0) * 0.55;
                light = directional * (0.25 + 0.75 * blockLight / 15.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 330 core
            in float light;
            uniform vec3 modelColor;
            out vec4 fragmentColor;
            void main() {
                fragmentColor = vec4(modelColor * light, 1.0);
            }
            """;

    public void initialize() {
        checkThread();
        if (initialized) throw new IllegalStateException("Renderer already initialized");

        shader = resources.register(new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER));
        cameraOffsetLocation = shader.uniformLocation("cameraOffset");
        zoomLocation = shader.uniformLocation("cameraZoom");
        modelOffsetLocation = shader.uniformLocation("modelOffset");
        modelScaleLocation = shader.uniformLocation("modelScale");
        modelColorLocation = shader.uniformLocation("modelColor");

        blockRegistry = BlockRegistry.builder()
                .register(new BlockDefinition(1, RenderLayer.OPAQUE, true, true))
                .register(new BlockDefinition(2, RenderLayer.OPAQUE, true, true))
                .build();
        meshingService = new ChunkMeshingService(2, 16);
        streamingController = new ChunkStreamingController(world, 2, 16, 1, chunkStore);
        refreshVisibleMeshes(true);
        ChunkSection playerCube = new ChunkSection();
        playerCube.setBlockId(0, 0, 0, 1);
        ChunkMesh playerCubeMesh = new ChunkMesher().build(playerCube, 0, 0, 0,
                blockRegistry, (x, y, z) -> BlockDefinition.AIR_ID, RenderLayer.OPAQUE);
        playerMesh = resources.register(new OpenGLChunkMesh(playerCubeMesh, 0, 0, 0));
        hud = resources.register(new OpenGLHud());

        initialized = true;
    }

    private void refreshVisibleMeshes(boolean rebuildAll) {
        int playerBlockX = (int) Math.floor(playerX);
        int playerBlockZ = (int) Math.floor(playerZ);
        streamingController.updateAround(playerBlockX, playerBlockZ, VIEW_DISTANCE_CHUNKS);
        var visible = ChunkVisibility.withinRadius(world.snapshotSections(),
                playerBlockX, playerBlockZ, VIEW_DISTANCE_CHUNKS);
        Set<ChunkPos> target = new HashSet<>(visible);

        for (Iterator<Map.Entry<ChunkPos, OpenGLChunkMesh>> iterator = chunkMeshes.entrySet().iterator();
             iterator.hasNext();) {
            Map.Entry<ChunkPos, OpenGLChunkMesh> entry = iterator.next();
            if (!target.contains(entry.getKey())) {
                resources.release(entry.getValue());
                iterator.remove();
            }
        }
        for (Iterator<Map.Entry<ChunkPos, CompletableFuture<ChunkMeshingService.MeshResult>>> iterator =
             pendingMeshes.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<ChunkPos, CompletableFuture<ChunkMeshingService.MeshResult>> entry = iterator.next();
            if (!target.contains(entry.getKey()) || rebuildAll) {
                entry.getValue().cancel(false);
                if (!target.contains(entry.getKey())) meshingService.invalidate(entry.getKey());
                iterator.remove();
            }
        }
        if (rebuildAll) {
            lightMap = LightMap.maximum(new BlockLightEngine().compute(world, blockRegistry),
                    new SkyLightEngine().compute(world, blockRegistry));
        }
        for (ChunkPos position : visible) {
            if (!rebuildAll && (chunkMeshes.containsKey(position) || pendingMeshes.containsKey(position))) continue;
            CompletableFuture<ChunkMeshingService.MeshResult> previous = pendingMeshes.remove(position);
            if (previous != null) previous.cancel(false);
            ChunkSection section = world.get(position);
            if (section == null) {
                OpenGLChunkMesh old = chunkMeshes.remove(position);
                if (old != null) resources.release(old);
                meshingService.invalidate(position);
                continue;
            }
            pendingMeshes.put(position, meshingService.submit(position, section, blockRegistry,
                    world, RenderLayer.OPAQUE, lightMap));
        }
        preparedWorldRevision = world.revision();
        visibleRegionDirty = false;
    }

    public void render(int framebufferWidth, int framebufferHeight, float deltaSeconds) {
        checkThread();
        if (!initialized) throw new IllegalStateException("Renderer is not initialized");
        if (framebufferWidth <= 0 || framebufferHeight <= 0) return;

        glViewport(0, 0, framebufferWidth, framebufferHeight);
        glClearColor(0.035f, 0.045f, 0.075f, 1.0f);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        streamingController.updateAround((int) Math.floor(playerX), (int) Math.floor(playerZ),
                VIEW_DISTANCE_CHUNKS);
        boolean worldChanged = world.revision() != preparedWorldRevision;
        if (worldChanged || visibleRegionDirty) refreshVisibleMeshes(worldChanged);

        for (Iterator<Map.Entry<ChunkPos, CompletableFuture<ChunkMeshingService.MeshResult>>> iterator =
             pendingMeshes.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<ChunkPos, CompletableFuture<ChunkMeshingService.MeshResult>> entry = iterator.next();
            CompletableFuture<ChunkMeshingService.MeshResult> pending = entry.getValue();
            if (pending.isDone()) {
                ChunkMeshingService.MeshResult result = pending.join();
                if (meshingService.isCurrent(result)) {
                    ChunkPos position = result.position();
                    OpenGLChunkMesh old = chunkMeshes.remove(position);
                    if (old != null) resources.release(old);
                    if (!result.mesh().isEmpty()) {
                        OpenGLChunkMesh mesh = resources.register(new OpenGLChunkMesh(result.mesh(),
                                position.originX(), position.originY(), position.originZ()));
                        chunkMeshes.put(position, mesh);
                    }
                }
                iterator.remove();
            }
        }

        shader.bind();
        glUniform2f(cameraOffsetLocation, camera.offsetX(), camera.offsetY());
        glUniform1f(zoomLocation, camera.zoom());
        glUniform3f(modelOffsetLocation, 0.0f, 0.0f, 0.0f);
        glUniform3f(modelScaleLocation, 1.0f, 1.0f, 1.0f);
        glUniform3f(modelColorLocation, 0.20f, 0.66f, 0.34f);
        for (OpenGLChunkMesh mesh : chunkMeshes.values()) mesh.draw();
        glUniform3f(modelOffsetLocation, playerX - 0.3f, playerY, playerZ - 0.3f);
        glUniform3f(modelScaleLocation, 0.6f, 1.8f, 0.6f);
        glUniform3f(modelColorLocation, 0.95f, 0.58f, 0.18f);
        playerMesh.draw();
        glUseProgram(0);
        hud.draw(framebufferWidth, framebufferHeight, selectedHotbarSlot, hotbarOccupied,
                inventoryItemIds, inventoryCounts, inventoryOpen, craftingItemIds, craftingCounts,
                craftingResultItemId, craftingResultCount, selectedCraftSlot, editorActions,
                editorBindings, selectedEditorRow, editorOpen, editorCapturing);
    }

    public void setHotbarState(int selectedSlot, boolean[] occupied) {
        checkThread();
        java.util.Objects.requireNonNull(occupied, "occupied");
        if (selectedSlot < 0 || selectedSlot >= 9 || occupied.length != 9) {
            throw new IllegalArgumentException("Hotbar requires a selected slot and nine occupancy flags");
        }
        selectedHotbarSlot = selectedSlot;
        hotbarOccupied = occupied.clone();
    }

    @Override
    public String storageError() {
        return streamingController == null ? "" : streamingController.storageError();
    }

    @Override
    public void setInventoryState(int[] itemIds, int[] counts, int selectedSlot, boolean open,
                                  int[] craftingIds, int[] craftingStackCounts, int resultItemId,
                                  int resultStackCount, int selectedInputSlot) {
        checkThread();
        if (itemIds == null || counts == null || itemIds.length != 36 || counts.length != 36
                || craftingIds == null || craftingStackCounts == null
                || craftingIds.length != 4 || craftingStackCounts.length != 4) {
            throw new IllegalArgumentException("Inventory state requires 36 inventory and four crafting stacks");
        }
        if (selectedSlot < 0 || selectedSlot >= 9) throw new IllegalArgumentException("Selected slot must be 0..8");
        if (selectedInputSlot < -1 || selectedInputSlot >= 4) {
            throw new IllegalArgumentException("Selected crafting slot must be -1..3");
        }
        inventoryItemIds = itemIds.clone();
        inventoryCounts = counts.clone();
        craftingItemIds = craftingIds.clone();
        craftingCounts = craftingStackCounts.clone();
        craftingResultItemId = resultItemId;
        craftingResultCount = resultStackCount;
        selectedHotbarSlot = selectedSlot;
        inventoryOpen = open;
        selectedCraftSlot = selectedInputSlot;
    }

    @Override
    public void setKeyBindingEditorState(String[] actions, String[] bindings, int selectedRow,
                                         boolean open, boolean capturing) {
        checkThread();
        if (actions == null || bindings == null || actions.length == 0 || actions.length != bindings.length
                || selectedRow < 0 || selectedRow >= actions.length) {
            throw new IllegalArgumentException("Invalid keybinding editor state");
        }
        editorActions = actions.clone();
        editorBindings = bindings.clone();
        selectedEditorRow = selectedRow;
        editorOpen = open;
        editorCapturing = capturing;
    }

    public void setPlayerPosition(double x, double y, double z) {
        checkThread();
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Player position must be finite");
        }
        playerX = (float) x;
        playerY = (float) y;
        playerZ = (float) z;
        ChunkPos playerChunk = ChunkPos.fromBlock((int) Math.floor(playerX), 0,
                (int) Math.floor(playerZ));
        if (playerChunk.x() != playerChunkX || playerChunk.z() != playerChunkZ) {
            playerChunkX = playerChunk.x();
            playerChunkZ = playerChunk.z();
            visibleRegionDirty = true;
        }
    }

    public Optional<BlockRaycaster.Hit> pickBlock(double cursorX, double cursorY,
                                                   int windowWidth, int windowHeight) {
        checkThread();
        if (!initialized) throw new IllegalStateException("Renderer is not initialized");
        if (windowWidth <= 0 || windowHeight <= 0
                || !Double.isFinite(cursorX) || !Double.isFinite(cursorY)) return Optional.empty();
        double ndcX = cursorX / windowWidth * 2.0 - 1.0;
        double ndcY = 1.0 - cursorY / windowHeight * 2.0;
        double scale = 0.04 * camera.zoom();
        double projectedX = (ndcX - camera.offsetX()) / scale;
        double projectedY = (ndcY - camera.offsetY()) / scale;
        double cameraY = Math.max(512.0, playerY + 128.0);
        double centeredSum = 2.0 * (projectedY + cameraY * 1.25);
        double originX = 8.0 + (centeredSum + projectedX) * 0.5;
        double originZ = 8.0 + (centeredSum - projectedX) * 0.5;
        return new BlockRaycaster().cast(world, blockRegistry, originX, cameraY, originZ,
                -1.25, -1.0, -1.25, 2048.0);
    }

    public void moveCamera(float deltaX, float deltaY) {
        checkThread();
        camera.pan(deltaX, deltaY);
    }

    public void changeZoom(float delta) {
        checkThread();
        camera.zoom(delta);
    }

    private void checkThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("Renderer must be used on its OpenGL context thread");
        }
    }

    @Override
    public void close() {
        checkThread();
        if (streamingController != null) streamingController.close();
        if (meshingService != null) meshingService.close();
        for (CompletableFuture<?> pending : pendingMeshes.values()) pending.cancel(false);
        pendingMeshes.clear();
        resources.close();
        chunkMeshes.clear();
        initialized = false;
    }

}
