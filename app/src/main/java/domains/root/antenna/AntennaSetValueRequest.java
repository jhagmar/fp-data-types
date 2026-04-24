package domains.root.antenna;

import execution.TraceIdFactory;

import java.util.Optional;

public final class AntennaSetValueRequest extends AntennaRequest {

    private final String traceId;
    private final int value;

    private AntennaSetValueRequest(String traceId, int value) {
        this.traceId = traceId;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    @FunctionalInterface
    public interface Factory {
        Optional<AntennaSetValueRequest> create(int value);
    }

    public static Factory getFactory(TraceIdFactory traceIdFactory) {
        return (value) ->
                AntennaSetValueRequest.of(traceIdFactory.createTraceId(), value);
    }

    private static Optional<AntennaSetValueRequest> of(String traceId,
                                                            int value) {
        if (traceId == null || traceId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new AntennaSetValueRequest(traceId, value));
    }
}