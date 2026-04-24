package execution;

/**
 * A strategic boundary interface responsible for generating unique execution trace identifiers.
 * <p>
 * By abstracting trace ID generation, the execution engine maintains strict control over
 * correlation context. This prevents external clients in the "DMZ" from forging or
 * manipulating trace IDs, and allows the system to easily hot-swap ID generation
 * strategies in the future.
 * </p>
 */
@FunctionalInterface
public interface TraceIdFactory {

    /**
     * Generates a new, guaranteed-unique trace identifier.
     *
     * @return a strictly non-null, non-blank correlation ID string
     */
    String createTraceId();
}