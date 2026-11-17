package dev.aether.client.world.chunk;

import dev.aether.client.world.BlockAccess;
import dev.aether.client.world.BlockRegistry;
import dev.aether.client.world.LightMap;
import dev.aether.client.world.RenderLayer;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** CPU-only chunk meshing with bounded work queue and per-chunk stale-result suppression. */
public final class ChunkMeshingService implements AutoCloseable {
    private final ThreadPoolExecutor workers;
    private final ChunkMesher mesher = new ChunkMesher();
    private final ConcurrentMap<ChunkPos, AtomicLong> revisions = new ConcurrentHashMap<>();
    private volatile boolean closed;

    public ChunkMeshingService(int workerCount, int queueCapacity) {
        if (workerCount <= 0) throw new IllegalArgumentException("workerCount must be positive");
        if (queueCapacity <= 0) throw new IllegalArgumentException("queueCapacity must be positive");
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicLong ids = new AtomicLong();

            @Override
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "aether-chunk-mesher-" + ids.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        workers = new ThreadPoolExecutor(workerCount, workerCount, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    public CompletableFuture<MeshResult> submit(
            ChunkPos position,
            ChunkSection source,
            BlockRegistry registry,
            BlockAccess blockAccess,
            RenderLayer layer
    ) {
        return submit(position, source, registry, blockAccess, layer, LightMap.FULL_BRIGHT);
    }

    public CompletableFuture<MeshResult> submit(
            ChunkPos position,
            ChunkSection source,
            BlockRegistry registry,
            BlockAccess blockAccess,
            RenderLayer layer,
            LightMap lightMap
    ) {
        if (closed) throw new IllegalStateException("Chunk meshing service is closed");
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(registry, "registry");
        Objects.requireNonNull(blockAccess, "blockAccess");
        Objects.requireNonNull(layer, "layer");
        Objects.requireNonNull(lightMap, "lightMap");

        long revision = revisions.computeIfAbsent(position, ignored -> new AtomicLong()).incrementAndGet();
        ChunkSection snapshot = source.snapshot();
        BlockAccess stableBlockAccess = blockAccess instanceof ChunkWorld world ? world.snapshot() : blockAccess;
        CompletableFuture<MeshResult> future = new CompletableFuture<>();
        try {
            workers.execute(() -> {
                if (!isCurrent(position, revision)) {
                    future.cancel(false);
                    return;
                }
                try {
                    ChunkMesh mesh = mesher.build(snapshot, position.originX(), position.originY(),
                            position.originZ(), registry, stableBlockAccess, layer, lightMap);
                    if (isCurrent(position, revision)) future.complete(new MeshResult(position, revision, mesh));
                    else future.cancel(false);
                } catch (Throwable failure) {
                    future.completeExceptionally(failure);
                }
            });
        } catch (RuntimeException rejected) {
            future.completeExceptionally(rejected);
        }
        return future;
    }

    public void invalidate(ChunkPos position) {
        revisions.computeIfAbsent(Objects.requireNonNull(position, "position"), ignored -> new AtomicLong())
                .incrementAndGet();
    }

    public boolean isCurrent(MeshResult result) {
        Objects.requireNonNull(result, "result");
        return isCurrent(result.position(), result.revision());
    }

    private boolean isCurrent(ChunkPos position, long revision) {
        AtomicLong current = revisions.get(position);
        return !closed && current != null && current.get() == revision;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        workers.shutdownNow();
        revisions.clear();
    }

    public record MeshResult(ChunkPos position, long revision, ChunkMesh mesh) {
        public MeshResult {
            Objects.requireNonNull(position, "position");
            Objects.requireNonNull(mesh, "mesh");
        }
    }
}
