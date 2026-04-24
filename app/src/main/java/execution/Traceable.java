package execution;

/**
 * A contract indicating that an object participates in the engine's distributed tracing mechanism.
 * <p>
 * Implementing this interface ensures that commands, events, or state changes can be
 * correlated back to a single, unified execution flow. This is critical for logging,
 * auditing, and debugging complex asynchronous task graphs.
 * </p>
 */
public interface Traceable {

    /**
     * Retrieves the unique execution trace identifier.
     *
     * @return the string representation of the correlation or trace ID
     */
    String getTraceId();
}
