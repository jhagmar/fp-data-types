package domains.root.antenna;

import domains.ExecutionOutcome;

import java.util.Objects;

/**
 * A concrete, immutable outcome representing a finalized change in the physical antenna's status.
 * <p>
 * Handlers return this record after successfully processing an {@code AntennaElementStatusRequest}.
 * This object essentially acts as a "diff" or a new snapshot payload. The engine will
 * subsequently use this outcome to update the {@link AntennaVolatileStatePart} via
 * the application's Lens registry, executing a thread-safe Compare-And-Swap (CAS) operation.
 * </p>
 *
 * @param getTraceId             the correlation identifier carried over from the originating request
 * @param element0Operational the finalized operational status of element 0
 * @param element1Operational the finalized operational status of element 1
 */
public record AntennaElementStatusExecutionOutcome(
        String getTraceId,
        boolean element0Operational,
        boolean element1Operational
) implements ExecutionOutcome<AntennaScope> {

    /**
     * Compact constructor to enforce strict internal invariants.
     * Ensures that no handler accidentally drops the tracing context before
     * the outcome reaches the state-mutation layer.
     */
    public AntennaElementStatusExecutionOutcome {
        Objects.requireNonNull(getTraceId, "Execution outcome must retain its traceId");
        if (getTraceId.isBlank()) {
            throw new IllegalArgumentException("Execution outcome traceId cannot be blank");
        }
    }

}