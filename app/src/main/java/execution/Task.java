package execution;

/**
 * Represents a discrete unit of work within the execution engine.
 * <p>
 * A task serves as an aggregate of an executable action ({@link Resolvable}),
 * execution context tracking ({@link Traceable}), and the specific data
 * payload required to perform the operation.
 * </p>
 *
 * @param <Payload> the type of the data structure carrying the task's input or context
 */
public interface Task<Payload> extends Resolvable, Traceable {

    /**
     * Retrieves the underlying data required to execute this task.
     *
     * @return the task's payload
     */
    public abstract Payload getPayload();

}