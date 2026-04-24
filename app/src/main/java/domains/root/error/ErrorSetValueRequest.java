package domains.root.error;

import execution.TraceIdFactory;

import java.util.Optional;

public final class ErrorSetValueRequest extends ErrorRequest {

    private final String traceId;
    private final int value;

    private ErrorSetValueRequest(String traceId, int value) {
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
        Optional<ErrorSetValueRequest> create(int value);
    }

    public static Factory getFactory(TraceIdFactory traceIdFactory) {
        return (value) ->
                ErrorSetValueRequest.of(traceIdFactory.createTraceId(), value);
    }

    private static Optional<ErrorSetValueRequest> of(String traceId,
                                                            int value) {
        if (traceId == null || traceId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new ErrorSetValueRequest(traceId, value));
    }
}