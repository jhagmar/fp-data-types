package execution;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A thread-safe, array-backed implementation of a {@link WorkQueue} with a
 * fixed maximum capacity.
 *
 * @param <Payload> The type of work item.
 */
public final class BoundedWorkQueue<Payload extends Traceable> implements WorkQueue<Payload> {

    private final BlockingQueue<Task<Payload>> queue;

    private BoundedWorkQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    /**
     * Creates a new work queue with the specified maximum capacity.
     *
     * @param capacity The maximum number of items the queue can hold.
     * @param <Payload> The type of work item.
     * @return A new instance of {@code BoundedWorkQueue}.
     * @throws IllegalArgumentException if capacity is less than 1.
     */
    public static <Payload extends ConflictAware<Payload> & Traceable> BoundedWorkQueue<Payload> create(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        return new BoundedWorkQueue<>(capacity);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses {@link BlockingQueue#offer(Object)} to ensure
     * the call does not block if the buffer is full.
     * </p>
     */
    @Override
    public boolean tryEnqueue(Task<Payload> payload) {
        Objects.requireNonNull(payload, "Cannot enqueue null items");
        return queue.offer(payload);
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses {@link BlockingQueue#take()} to block the
     * calling thread until work is available.
     * </p>
     */
    @Override
    public Task<Payload> take() throws InterruptedException {
        return queue.take();
    }
}