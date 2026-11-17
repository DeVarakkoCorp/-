# Aether Client

An independent Minecraft Java Edition 1.20.6-inspired client prototype. This repository is a working foundation and is **not yet a Minecraft-compatible or playable replacement**.

## Implemented vertical slice

- Java 21, Gradle Kotlin DSL, LWJGL 3.3.3, GLFW and an OpenGL 3.3 Core renderer.
- Window and context lifecycle, indexed chunk meshes, depth-tested isometric terrain rendering, and a visible player mesh.
- Chunk block storage, negative world coordinates, neighbor-aware face culling, demo terrain generation, and bounded asynchronous meshing with stale-result suppression.
- Player-centered visibility with bounded asynchronous terrain generation and a dedicated chunk-storage thread. Sections leaving the visible window are saved individually with checksums and atomic replacement, then unloaded after a successful save; returning sections reload asynchronously before generation. Demo terrain generation remains bounded to eight chunks from world origin.
- CPU block-light flood fill and top-down direct sky light, combined and sampled into chunk mesh vertices.
- Fixed-step 20 Hz simulation, basic AABB player movement, gravity, jumping, and collision against solid blocks.
- Keyboard and mouse input is routed through action bindings, allowing actions to be reassigned in code; the launcher uses the documented defaults.
- A 36-slot inventory with a nine-slot hotbar is connected to block interaction: mining collects a block, placement consumes the selected stack, and number keys `1`–`9` select hotbar slots. Press `E` to open the inventory, then click a main-inventory stack to swap it with the selected hotbar slot. The 2×2 crafting panel converts four Stone into one Polished Stone block. The selected slot and stack count appear in the window title.
- The inventory window title shows the readable name of the stack under the pointer, including crafting inputs and results.
- Player position, selected hotbar slot, and all inventory stacks persist in a checksummed, atomically replaced player profile.
- Keyboard and mouse bindings persist in the readable `saves/default/keybindings.properties` file. Edit entries such as `MOVE_FORWARD=key:W` or `MINE=mouse:LEFT`; duplicate or unknown inputs are rejected on load.
- Mouse selection uses grid traversal; holding left mouse mines after each block type's configured mining time (0.5 seconds for Stone and 1.5 seconds for Polished Stone), while right click places the selected block item beside it. Edits update world revisions and placement checks player collision.
- Mining progress follows a stable ray target, resets when the target changes or the button is released, caps long-frame progress, and appears in the window title.
- A bounded Java status client sends the 1.20.6 handshake (protocol 766), returns raw status JSON, and measures ping/pong latency. This is server-list status only; it does not authenticate or enter play state.
- World revisions trigger light recomputation and visible-chunk remeshing after changes through `ChunkWorld`; replaced GPU meshes are released immediately. Versioned save files use CRC validation and atomic replacement.
- The launcher loads and saves `saves/default/world.aether`.
- OpenGL resource cleanup, framebuffer/render-target implementation, and shader include loading with traversal and cycle checks.
- A backend-neutral `ClientRenderer` contract and `RendererFactory`; the launcher selects the OpenGL implementation through its factory.
- Startup diagnostics for OpenGL version, driver, texture limit, color attachments, and sample count.
- A nine-slot hotbar is drawn over the world; its selected slot and occupied items update as the player changes inventory state.
- 117 unit tests for CPU world/lighting systems, block selection, per-block mining time and interaction, item names, crafting, action bindings and persistence, inventory/profile persistence, hotbar and inventory layout, repeated asynchronous chunk unload/reload persistence, protocol framing, status exchange, meshing, and lifecycle logic.

## Build and run on this Windows workspace

JDK 21 and a Gradle 8.14.3 wrapper are included in the workspace setup. In PowerShell:

```powershell
. C:\чатгпт\AetherClient\dev-env.ps1
Invoke-AetherGradle clean build
Invoke-AetherGradle run
```

The helper configures the local JDK and temporary directory. Press **Escape** to close the client. The window uses the system's OpenGL driver and Windows x64 LWJGL natives.

For another environment, use Java 21 and run `gradlew.bat clean build` (Windows) or `./gradlew clean build` (Linux/macOS), setting the LWJGL native classifier as needed with `-PlwjglNatives=...`.

Create a clean source archive and SHA-256 manifest beside the project with `python tools/package_source.py`.

## Controls

- `W`, `A`, `S`, `D`: move the player.
- `Space`: jump.
- `1`–`9`: select a hotbar slot.
- Hold left mouse: mine the selected block.
- Right mouse: place the selected block beside the targeted block.
- Arrow keys: pan the view.
- `+` / `-`: zoom.
- `Escape`: exit and save the world.

## Project layout

- `src/main/java/dev/aether/client/launcher`: application entry point and loop.
- `platform`, `game`, `core`: window, player simulation, and fixed-step timing.
- `rendering` and `rendering/opengl`: camera, render targets, shader program, chunk GPU meshes, and resource lifecycle.
- `network`: bounded protocol framing and the Java server status query.
- `world` and `world/chunk`: block registry, chunk storage, meshing, worker service, demo generation, and persistence.
- `game`: movement, interactions, action bindings, inventory, and player profile persistence.
- `src/test`: CPU-side JUnit 5 tests.
- `docs`: architecture and verified implementation status.
- `docs/PROTOCOL_SOURCES.md`: public protocol references used for the server-list status slice.

## Not implemented

Minecraft login/authentication and play-state protocol compatibility, real game data/resource packs, indirect/colored and incremental lighting, full gameplay, audio/UI, Vulkan, a stable mod API, and Fabric/Forge compatibility remain future work. The current demo terrain is generated locally. Performance and graphics quality have not been measured on the target Huawei laptop.

## Clean-room and licensing

The code is an independent implementation and must not include decompiled Mojang code or redistribute protected game assets. Any official game data must be obtained lawfully by the user. No compatibility with third-party mods is promised.
