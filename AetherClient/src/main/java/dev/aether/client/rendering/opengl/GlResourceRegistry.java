package dev.aether.client.rendering.opengl;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class GlResourceRegistry implements AutoCloseable {
    private final Thread ownerThread;
    private final List<AutoCloseable> resources = new ArrayList<>();
    private boolean closed;

    public GlResourceRegistry(Thread ownerThread) {
        this.ownerThread = Objects.requireNonNull(ownerThread, "ownerThread");
    }

    public <T extends AutoCloseable> T register(T resource) {
        checkThread();
        Objects.requireNonNull(resource, "resource");
        if (closed) {
            throw new IllegalStateException("Resource registry is closed");
        }
        resources.add(resource);
        return resource;
    }

    public int resourceCount() {
        checkThread();
        return resources.size();
    }

    public void release(AutoCloseable resource) {
        checkThread();
        Objects.requireNonNull(resource, "resource");
        if (closed) throw new IllegalStateException("Resource registry is closed");
        int index = resources.lastIndexOf(resource);
        if (index < 0) return;
        try {
            resource.close();
        } catch (Exception failure) {
            throw new RuntimeException("Could not release OpenGL resource", failure);
        }
        resources.remove(index);
    }

    private void checkThread() {
        if (Thread.currentThread() != ownerThread) {
            throw new IllegalStateException("OpenGL resources must be managed on the context thread");
        }
    }

    @Override
    public void close() {
        checkThread();
        if (closed) return;
        closed = true;

        RuntimeException aggregate = null;
        for (int i = resources.size() - 1; i >= 0; i--) {
            try {
                resources.get(i).close();
            } catch (Exception e) {
                if (aggregate == null) {
                    aggregate = new RuntimeException("One or more OpenGL resources failed to close");
                }
                aggregate.addSuppressed(e);
            }
        }
        resources.clear();
        if (aggregate != null) throw aggregate;
    }
}
