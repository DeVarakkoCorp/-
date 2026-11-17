package dev.aether.client.world.chunk;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded asynchronous generation for the local demo terrain. */
public final class ChunkGenerationService implements AutoCloseable {
    private final ThreadPoolExecutor workers;
    private final ConcurrentMap<ChunkPos, Long> revisions = new ConcurrentHashMap<>();
    private final AtomicLong revisionSequence = new AtomicLong();
    private volatile boolean closed;

    public ChunkGenerationService(int workerCount, int queueCapacity) {
        if (workerCount <= 0) throw new IllegalArgumentException("workerCount must be positive");
        if (queueCapacity <= 0) throw new IllegalArgumentException("queueCapacity must be positive");
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicLong ids = new AtomicLong();

            @Override
            public Thread newThread(Runnable task) {
                Thread thread = new Thread(task, "aether-chunk-generator-" + ids.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        };
        workers = new ThreadPoolExecutor(workerCount, workerCount, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueCapacity), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    public CompletableFuture<GeneratedChunk> submit(ChunkPos position, int seed) {
        if (closed) throw new IllegalStateException("Chunk generation service is closed");
        Objects.requireNonNull(position, "position");
        if (!DemoWorldGenerator.canGenerate(position)) {
            throw new IllegalArgumentException("Chunk is outside the demo generator bounds: " + position);
        }
        long revision = revisionSequence.incrementAndGet();
        revisions.put(position, revision);
        CompletableFuture<GeneratedChunk> future = new CompletableFuture<>();
        try {
            workers.execute(() -> {
                if (!isCurrent(position, revision)) {
                    future.cancel(false);
                    return;
                }
                try {
                    ChunkSection section = DemoWorldGenerator.generateSection(position, seed);
                    if (isCurrent(position, revision)) future.complete(new GeneratedChunk(position, revision, section));
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
        revisions.remove(Objects.requireNonNull(position, "position"));
        revisionSequence.incrementAndGet();
    }

    public void retire(ChunkPos position, long revision) {
        revisions.remove(Objects.requireNonNull(position, "position"), revision);
    }

    public boolean isCurrent(GeneratedChunk generated) {
        Objects.requireNonNull(generated, "generated");
        Long current = revisions.get(generated.position());
        return !closed && current != null && current == generated.revision();
    }

    private boolean isCurrent(ChunkPos position, long revision) {
        Long current = revisions.get(position);
        return !closed && current != null && current == revision;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        workers.shutdownNow();
        revisions.clear();
    }

    public record GeneratedChunk(ChunkPos position, long revision, ChunkSection section) {
        public GeneratedChunk {
            Objects.requireNonNull(position, "position");
            Objects.requireNonNull(section, "section");
        }
    }
}
