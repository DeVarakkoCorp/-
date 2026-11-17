package dev.aether.client.rendering.opengl;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GlResourceRegistryTest {
    @Test
    void closesResourcesInReverseRegistrationOrder() {
        List<Integer> closed = new ArrayList<>();
        GlResourceRegistry registry = new GlResourceRegistry(Thread.currentThread());
        registry.register(() -> closed.add(1));
        registry.register(() -> closed.add(2));
        registry.register(() -> closed.add(3));

        registry.close();

        assertEquals(List.of(3, 2, 1), closed);
        assertEquals(0, registry.resourceCount());
    }

    @Test
    void releasesAndForgetsAnIndividualResource() {
        List<Integer> closed = new ArrayList<>();
        GlResourceRegistry registry = new GlResourceRegistry(Thread.currentThread());
        AutoCloseable resource = () -> closed.add(1);
        registry.register(resource);

        registry.release(resource);

        assertEquals(List.of(1), closed);
        assertEquals(0, registry.resourceCount());
        registry.close();
        assertEquals(List.of(1), closed);
    }

    @Test
    void attemptsEveryCloseAndAggregatesFailures() {
        List<Integer> closed = new ArrayList<>();
        GlResourceRegistry registry = new GlResourceRegistry(Thread.currentThread());
        registry.register(() -> closed.add(1));
        registry.register(() -> {
            closed.add(2);
            throw new IllegalStateException("second");
        });
        registry.register(() -> {
            closed.add(3);
            throw new IllegalArgumentException("third");
        });

        RuntimeException failure = assertThrows(RuntimeException.class, registry::close);

        assertEquals(List.of(3, 2, 1), closed);
        assertEquals(2, failure.getSuppressed().length);
        assertEquals(0, registry.resourceCount());
    }

    @Test
    void enforcesOwnerThreadAndClosedState() throws InterruptedException {
        GlResourceRegistry registry = new GlResourceRegistry(Thread.currentThread());
        AtomicReference<Throwable> threadFailure = new AtomicReference<>();
        Thread other = new Thread(() -> {
            try {
                registry.resourceCount();
            } catch (Throwable failure) {
                threadFailure.set(failure);
            }
        });
        other.start();
        other.join();

        assertInstanceOf(IllegalStateException.class, threadFailure.get());
        registry.close();
        assertDoesNotThrow(registry::close);
        assertThrows(IllegalStateException.class, () -> registry.register(() -> {}));
    }

    @Test
    void rejectsNullOwnerAndResource() {
        assertThrows(NullPointerException.class, () -> new GlResourceRegistry(null));
        GlResourceRegistry registry = new GlResourceRegistry(Thread.currentThread());

        assertThrows(NullPointerException.class, () -> registry.register(null));
        registry.close();
    }
}
