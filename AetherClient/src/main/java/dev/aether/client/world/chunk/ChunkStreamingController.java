package dev.aether.client.world.chunk;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

/** Maintains a bounded asynchronous generation window around the player's current chunk. */
public final class ChunkStreamingController implements AutoCloseable {
    private final ChunkWorld world;
    private final ChunkGenerationService generationService;
    private final int seed;
    private final ChunkStore store;
    private final ExecutorService storageExecutor;
    private final Map<ChunkPos, CompletableFuture<ChunkGenerationService.GeneratedChunk>> pending = new HashMap<>();
    private final Map<ChunkPos, CompletableFuture<java.util.Optional<ChunkSection>>> pendingLoads = new HashMap<>();
    private final Map<ChunkPos, CompletableFuture<Void>> pendingSaves = new HashMap<>();
    private final Map<ChunkPos, Long> retryAfterNanos = new HashMap<>();
    private Set<ChunkPos> desired = Set.of();
    private String storageError = "";
    private boolean closed;

    public ChunkStreamingController(ChunkWorld world, int workerCount, int queueCapacity, int seed) {
        this(world, workerCount, queueCapacity, seed, null);
    }

    public ChunkStreamingController(ChunkWorld world, int workerCount, int queueCapacity, int seed,
                                    ChunkStore store) {
        this.world = Objects.requireNonNull(world, "world");
        this.generationService = new ChunkGenerationService(workerCount, queueCapacity);
        this.seed = seed;
        this.store = store;
        this.storageExecutor = store == null ? null : Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "aether-chunk-storage");
            thread.setDaemon(true);
            return thread;
        });
    }

    /** Updates requests and installs completed sections. Returns true when the world gained sections. */
    public boolean updateAround(int playerBlockX, int playerBlockZ, int radiusInChunks) {
        if (closed) throw new IllegalStateException("Chunk streaming controller is closed");
        var desiredColumns = ChunkVisibility.columnsAround(playerBlockX, playerBlockZ, radiusInChunks);
        Set<ChunkPos> nextDesired = new HashSet<>(desiredColumns);

        for (Iterator<Map.Entry<ChunkPos, CompletableFuture<ChunkGenerationService.GeneratedChunk>>> iterator =
             pending.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<ChunkPos, CompletableFuture<ChunkGenerationService.GeneratedChunk>> entry = iterator.next();
            if (!nextDesired.contains(entry.getKey())) {
                entry.getValue().cancel(false);
                generationService.invalidate(entry.getKey());
                iterator.remove();
            }
        }
        desired = Set.copyOf(nextDesired);
        boolean changed = false;

        if (store != null) {
            for (ChunkPos loaded : world.snapshotSections().keySet()) {
                if (desired.contains(loaded)) continue;
                if (pendingSaves.containsKey(loaded)) continue;
                if (isBackoffActive(loaded)) continue;
                ChunkSection snapshot = world.get(loaded).snapshot();
                CompletableFuture<Void> save = CompletableFuture.runAsync(() -> {
                    try {
                        store.save(loaded, snapshot);
                    } catch (IOException failure) {
                        throw new CompletionException(failure);
                    }
                }, storageExecutor);
                pendingSaves.put(loaded, save);
            }
            for (Iterator<Map.Entry<ChunkPos, CompletableFuture<Void>>> iterator = pendingSaves.entrySet().iterator();
                 iterator.hasNext();) {
                Map.Entry<ChunkPos, CompletableFuture<Void>> entry = iterator.next();
                if (!entry.getValue().isDone()) continue;
                try {
                    entry.getValue().join();
                    clearStorageFailure(entry.getKey());
                    if (!desired.contains(entry.getKey())) world.unload(entry.getKey());
                } catch (CompletionException failure) {
                    recordStorageFailure(entry.getKey(), "Could not persist chunk", failure);
                }
                iterator.remove();
            }
            for (Iterator<Map.Entry<ChunkPos, CompletableFuture<java.util.Optional<ChunkSection>>>> iterator =
                 pendingLoads.entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<ChunkPos, CompletableFuture<java.util.Optional<ChunkSection>>> entry = iterator.next();
                CompletableFuture<java.util.Optional<ChunkSection>> load = entry.getValue();
                if (!load.isDone()) continue;
                try {
                    java.util.Optional<ChunkSection> stored = load.join();
                    clearStorageFailure(entry.getKey());
                    if (desired.contains(entry.getKey()) && world.get(entry.getKey()) == null) {
                        if (stored.isPresent()) {
                            world.putSection(entry.getKey(), stored.get());
                            changed = true;
                        } else if (DemoWorldGenerator.canGenerate(entry.getKey())) {
                            pending.put(entry.getKey(), generationService.submit(entry.getKey(), seed));
                        }
                    }
                } catch (CompletionException failure) {
                    recordStorageFailure(entry.getKey(), "Could not load chunk", failure);
                }
                iterator.remove();
            }
        }

        for (ChunkPos position : desiredColumns) {
            if (world.get(position) != null || pending.containsKey(position) || pendingLoads.containsKey(position)) continue;
            if (store != null) {
                if (isBackoffActive(position)) continue;
                CompletableFuture<Void> previousSave = pendingSaves.get(position);
                CompletableFuture<java.util.Optional<ChunkSection>> load = (previousSave == null
                        ? CompletableFuture.completedFuture(null) : previousSave)
                        .thenApplyAsync(ignored -> {
                            try {
                                return store.load(position);
                            } catch (IOException failure) {
                                throw new CompletionException(failure);
                            }
                        }, storageExecutor);
                pendingLoads.put(position, load);
                continue;
            }
            if (DemoWorldGenerator.canGenerate(position)) {
                pending.put(position, generationService.submit(position, seed));
            }
        }

        for (Iterator<Map.Entry<ChunkPos, CompletableFuture<ChunkGenerationService.GeneratedChunk>>> iterator =
             pending.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<ChunkPos, CompletableFuture<ChunkGenerationService.GeneratedChunk>> entry = iterator.next();
            CompletableFuture<ChunkGenerationService.GeneratedChunk> future = entry.getValue();
            if (!future.isDone()) continue;
            try {
                ChunkGenerationService.GeneratedChunk generated = future.join();
                if (desired.contains(generated.position()) && generationService.isCurrent(generated)
                        && world.get(generated.position()) == null) {
                    world.putSection(generated.position(), generated.section());
                    changed = true;
                }
                generationService.retire(generated.position(), generated.revision());
            } catch (CompletionException failure) {
                if (failure.getCause() instanceof java.util.concurrent.RejectedExecutionException) {
                    generationService.invalidate(entry.getKey());
                    iterator.remove();
                    continue;
                }
                throw new IllegalStateException("Chunk generation failed for " + entry.getKey(), failure.getCause());
            } catch (java.util.concurrent.CancellationException ignored) {
                // A superseded request is discarded; the current window can request it again if needed.
            }
            iterator.remove();
        }
        return changed;
    }

    public int pendingCount() {
        return pending.size();
    }

    public Set<ChunkPos> desiredChunks() {
        return desired;
    }

    public String storageError() {
        return storageError;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        generationService.close();
        for (CompletableFuture<?> future : pending.values()) future.cancel(false);
        if (storageExecutor != null) {
            storageExecutor.shutdown();
            try {
                if (!storageExecutor.awaitTermination(30, TimeUnit.SECONDS)) storageExecutor.shutdownNow();
            } catch (InterruptedException interrupted) {
                storageExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        pending.clear();
        pendingLoads.clear();
        pendingSaves.clear();
        retryAfterNanos.clear();
        desired = Set.of();
    }

    private boolean isBackoffActive(ChunkPos position) {
        Long retryAt = retryAfterNanos.get(position);
        if (retryAt == null) return false;
        if (System.nanoTime() < retryAt) return true;
        retryAfterNanos.remove(position);
        return false;
    }

    private void clearStorageFailure(ChunkPos position) {
        retryAfterNanos.remove(position);
        storageError = "";
    }

    private void recordStorageFailure(ChunkPos position, String operation, CompletionException failure) {
        Throwable cause = failure.getCause() == null ? failure : failure.getCause();
        String detail = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
        storageError = operation + " " + position + ": " + detail;
        retryAfterNanos.put(position, System.nanoTime() + TimeUnit.SECONDS.toNanos(2));
    }
}
