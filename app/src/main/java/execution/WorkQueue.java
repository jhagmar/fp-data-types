package execution;

/**
 * A generic producer-consumer buffer for managing units of work.
 *
 * @param <Payload> The type of work item held in this queue.
 */
public interface WorkQueue<Payload extends Traceable> {

    /**
     * Attempts to insert the specified payload into the queue without blocking.
     *
     * @param payload The work payload to add.
     * @return {@code true} if the payload was added, {@code false} if the queue is full.
     */
    boolean tryEnqueue(Task<Payload> payload);

    /**
     * Retrieves and removes the head of the queue, waiting if necessary
     * until an element becomes available.
     *
     * @return The next work item.
     * @throws InterruptedException if interrupted while waiting.
     */
    Task<Payload> take() throws InterruptedException;
}