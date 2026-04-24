package domains.root.antenna;

import domains.Request;
import domains.Scope;
import execution.TraceIdFactory;

import java.util.Optional;

/**
 * A concrete, immutable request to update the operational status of a specific antenna element.
 * <p>
 * This class represents a validated command targeting the {@link AntennaScope}.
 * It enforces a strict "Demilitarized Zone" (DMZ) pattern: external clients cannot
 * instantiate this request directly or provide their own trace IDs. Instead, the system
 * must supply a pre-configured {@link Factory} to the DMZ, ensuring all requests are
 * safely constructed, validated, and inherently traceable before entering the execution engine.
 * </p>
 */
public final class AntennaElementStatusRequest extends AntennaRequest {

    private final String traceId;
    private final int element;
    private final boolean operational;

    /**
     * Private constructor to prevent direct instantiation.
     */
    private AntennaElementStatusRequest(String traceId, int element, boolean operational) {
        this.traceId = traceId;
        this.element = element;
        this.operational = operational;
    }

    /**
     * Retrieves the target antenna element index.
     *
     * @return the element index (0 or 1)
     */
    public int getElement() {
        return element;
    }

    /**
     * Retrieves the desired operational status for the element.
     *
     * @return {@code true} if setting to operational, {@code false} otherwise
     */
    public boolean isOperational() {
        return operational;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    /**
     * The DMZ-facing factory contract for creating status requests.
     * <p>
     * External controllers/handlers use this interface to request state changes without
     * needing access to the engine's internal tracing mechanics.
     * </p>
     */
    @FunctionalInterface
    public interface Factory {
        /**
         * Attempts to create a validated status request.
         *
         * @param element     the index of the hardware element to update
         * @param operational the desired operational status
         * @return an {@link Optional} containing the valid request, or empty if inputs are invalid
         */
        Optional<AntennaElementStatusRequest> create(int element, boolean operational);
    }

    /**
     * Bootstraps a request factory bound to a specific trace ID generation strategy.
     * <p>
     * This method is typically called once at application startup or boundary configuration
     * to wire the domain requests to the infrastructural tracing system.
     * </p>
     *
     * @param traceIdFactory the internal engine strategy for generating correlation IDs
     * @return a secure factory instance safe for use in the DMZ
     */
    public static Factory getFactory(TraceIdFactory traceIdFactory) {
        // Leverages lambda syntax since Factory is a functional interface
        return (element, operational) ->
                AntennaElementStatusRequest.of(traceIdFactory.createTraceId(), element, operational);
    }

    /**
     * Internal validator and instantiation gatekeeper.
     * <p>
     * Enforces exception-free validation for all inputs, including the internally generated trace ID.
     * </p>
     *
     * @param traceId     the generated identifier to correlate this execution flow
     * @param element     the index of the hardware element to update
     * @param operational the new operational status
     * @return an {@link Optional} containing the validated request, or empty if any constraint fails
     */
    private static Optional<AntennaElementStatusRequest> of(String traceId,
                                                            int element,
                                                            boolean operational) {
        // Enforce exception-free validation for all inputs
        if (traceId == null || traceId.isBlank()) {
            return Optional.empty();
        }
        if (element < 0 || element > 1) {
            return Optional.empty();
        }

        return Optional.of(new AntennaElementStatusRequest(traceId, element, operational));
    }
}