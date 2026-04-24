package execution;

import java.util.Objects;

/**
 * A worker that continuously polls for ready tasks and executes them.
 * <p>
 * Designed to be run within a thread pool or as a virtual thread. This worker
 * pulls tasks from a {@link WorkQueue}, delegates the work to an {@link Executor},
 * and ensures that tasks are marked as resolved upon completion or failure.
 * </p>
 * <p>
 * <b>Error Handling Strategy:</b>
 * If the underlying executor throws an unhandled exception, the worker catches it,
 * resolves the task (to prevent graph deadlocks), and moves on to the next task.
 * If the thread is interrupted, it interprets this as a shutdown signal and exits gracefully.
 * </p>
 *
 * @param <Payload> The type of the task payload.
 */
public class TaskWorker<Payload extends Traceable> implements Runnable {

    private static final System.Logger LOGGER = System.getLogger(TaskWorker.class.getName());

    private final WorkQueue<Payload> workQueue;
    private final Executor<Payload> executor;

    private TaskWorker(WorkQueue<Payload> workQueue, Executor<Payload> executor) {
        this.workQueue = workQueue;
        this.executor = executor;
    }

    /**
     * Creates a new task worker.
     *
     * @param workQueue The queue to poll ready tasks from.
     * @param executor  The executor containing the business logic to apply.
     * @param <P>       The specific payload type.
     * @return A new TaskWorker instance.
     */
    public static <P extends Traceable> TaskWorker<P> create(
            WorkQueue<P> workQueue,
            Executor<P> executor) {
        Objects.requireNonNull(workQueue, "workQueue cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");
        return new TaskWorker<>(workQueue, executor);
    }

    @Override
    public void run() {
        try {
            // Poll continuously until an interrupt signals a shutdown
            while (!Thread.currentThread().isInterrupted()) {

                // 1. Wait for work (blocks and throws InterruptedException if shutting down)
                Task<Payload> task = workQueue.take();

                try {
                    // 2. Execute the business logic
                    executor.execute(task.getPayload());

                } catch (InterruptedException e) {
                    // 3a. Task was interrupted mid-execution (shutdown signal).
                    // Rethrow to the outer catch block to terminate the loop.
                    throw e;

                } catch (Exception e) {
                    // 3b. Catch unexpected business logic errors.
                    // The executor should handle its own errors, but if one escapes,
                    // we log it and swallow it here so the worker thread survives.
                    LOGGER.log(System.Logger.Level.ERROR,
                               "Unhandled exception during task execution (Trace ID: {0})",
                               task.getTraceId(), e);

                } finally {
                    // 4. Always resolve the task.
                    // Whether it succeeded, failed, or was interrupted, we must resolve it
                    // so the Scheduler can unblock downstream dependencies or clean up.
                    task.resolve();
                }
            }
        } catch (InterruptedException ignored) {
            // Graceful shutdown sequence.
            // Restore the interrupt flag in case upstream thread pool managers check it.
            Thread.currentThread().interrupt();
            LOGGER.log(System.Logger.Level.DEBUG, "TaskWorker shutting down gracefully.");
        }
    }
}