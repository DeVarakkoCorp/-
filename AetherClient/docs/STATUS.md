# Project status — 2026-11-10

## Verified in this workspace

| Area | State |
|---|---|
| Build | `clean build` succeeds with the bundled JDK 21.0.13 and Gradle wrapper 8.14.3 |
| Unit tests | 117 JUnit tests pass |
| GLFW/OpenGL startup | `run` opens a 1280×720 window on this Windows environment; Escape exits cleanly |
| Scene | Indexed, depth-tested meshes render generated chunk terrain and a player cube |
| Simulation | Fixed 20 Hz stepping, bounded catch-up, basic movement, gravity, jumping, and block collision |
| Chunk pipeline | 16³ block sections, negative coordinates, face culling across section borders, bounded CPU workers, snapshot inputs, and stale-result checks |
| Visibility and generation | Player-centered window selects loaded sections and requests missing demo chunks on a bounded 2-worker/16-queue service. A dedicated storage thread atomically persists section snapshots and reads returning sections without blocking the render loop; an outgoing section stays loaded until its save succeeds. A 20-cycle asynchronous unload/reload stress test preserves edited blocks without storage errors. Request revision records are retired so tracking does not grow with travel. Generation remains limited to eight chunks from origin |
| Block interaction | Grid DDA picking selects visible solid blocks. Holding left mouse mines after capped stable-target progress using each block definition's mining time (0.5 seconds for Stone, 1.5 seconds for Polished Stone); right click places the selected stack. Edits use revision-tracked APIs, stale hits are rejected, and placement inside the player collision box is blocked |
| Input | Movement, camera, zoom, mining, placement, and inventory toggle route through platform-neutral action bindings. All bindings persist in a readable properties file and are loaded at startup. GLFW key/mouse callbacks are retained and released with the window |
| Inventory and crafting | 36 stacks with a nine-slot hotbar. Mining collects one matching block item if capacity exists; placement consumes the selected item only after a successful world edit. Number keys select slots. `E` opens a 36-slot OpenGL inventory with a 2×2 recipe grid; one Stone in each cell crafts one Polished Stone block. Ingredients return when the screen closes, and a full output inventory leaves the recipe unchanged. Clicking a main-inventory stack swaps it with the selected hotbar slot. The window title shows readable names for hovered inventory stacks, crafting ingredients, and results. Movement, camera, mining, and placement pause while the panel is open |
| Network | Java 1.20.6 status handshake (protocol 766), bounded framing/VarInt decoding, raw status JSON, and nonce-validated ping/pong are implemented. Automated loopback-server tests cover the exchange; no live external server was queried |
| Lighting/mesh invalidation | CPU block light and direct sky light are sampled per face. `ChunkWorld` revisions trigger visible mesh rebuilds and light recomputation; replacing meshes releases old GL objects. Direct mutations through raw `ChunkSection` references bypass revision tracking |
| Persistence | Versioned binary world format with CRC validation and atomic save; separate player profile stores position, selected slot, and all stacks. Chunk sections leaving the active window use individual checksummed files written on a dedicated storage thread; shutdown waits up to 30 seconds for queued storage work |
| Shader/resource lifecycle | Include expansion checks and render-thread-owned GL cleanup are implemented and unit tested |
| Framebuffer | Color/depth target with completeness validation and transactional resize; GL-context integration coverage remains limited |
| Renderer API | Launcher uses a backend-neutral client-renderer contract and factory; only OpenGL is implemented |
| Runtime visuals and interaction | Current build opens on Windows and renders terrain, player, and the hotbar; changing hotbar selection was visually verified. Inventory/crafting panel was visually confirmed, and mouse input successfully placed four ingredients and crafted Polished Stone. Hovering an occupied inventory slot updates the title with its readable item name. The world and original player inventory were restored after the runtime checks; player coordinates were preserved. Mouse inventory slot exchange and held mining have unit coverage but were not separately exercised in the desktop runtime. Target Huawei graphics hardware has not been tested |
| Device diagnostics | Startup logged OpenGL 3.3.0, Intel UHD Graphics, driver string `Build 32.0.101.7092`, 16,384 max texture size, 8 color attachments, and 16 samples on the current Windows environment; this does not establish that it is the target Huawei laptop |

## Not implemented

- Minecraft login/authentication, Play-state protocol, or compatibility with 1.20.6 gameplay servers; only server-list status is implemented.
- Real game assets/resource packs, indirect/colored lighting, incremental lighting, and a complete gameplay loop.
- Vulkan backend, production rendering abstraction, robust shader hot reload/render graph, and performance profiling.
- Mod API or Fabric/Forge adapters.
- Audio, menus, settings, and a production-ready input/UI system.

This is a functioning graphics/gameplay prototype, not a complete Minecraft client. Do not describe the remaining items as implemented or imply target-device performance without testing them.

## Next development priorities

1. Add a graphical keybinding editor; mappings can currently be edited in `saves/default/keybindings.properties`.
2. Stress repeated asynchronous unload/reload cycles and improve storage failure reporting; lighting currently recomputes across loaded sections after each world revision.
3. Expand the renderer seam into explicit render passes, then add and validate a Vulkan backend.
4. Extend the status transport into Login and Play states, then implement official session handling; keep lawful game-data acquisition separate from the client binary.
5. Add compatibility adapters and a documented mod API only after core interfaces stabilize.
6. Profile frame time, memory, chunk latency, and native resource behavior on the actual target Huawei laptop.

## Reproduction

From PowerShell in this workspace:

```powershell
. C:\чатгпт\AetherClient\dev-env.ps1
Invoke-AetherGradle clean build
Invoke-AetherGradle run
```
