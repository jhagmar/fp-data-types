package execution;

/**
 * Defines the execution logic for a specific task payload.
 * <p>
 * Implementations of this interface are responsible for executing the business
 * logic associated with a task and handling any domain-specific errors.
 * </p>
 *
 * @param <Payload> The type of the task payload.
 */
@FunctionalInterface
public interface Executor<Payload extends Traceable> {

    /**
     * Executes the given payload.
     *
     * @param payload The task payload to process.
     * @throws InterruptedException if the executing thread is interrupted
     * (e.g., during application shutdown).
     */
    void execute(Payload payload) throws InterruptedException;

}