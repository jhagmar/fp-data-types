package domains.root;

import execution.TraceIdFactory;

import java.util.Optional;

public final class RootAddValuesRequest extends RootRequest {

    private final String traceId;

    private RootAddValuesRequest(String traceId) {
        this.traceId = traceId;
    }

    @Override
    public String getTraceId() {
        return traceId;
    }

    @FunctionalInterface
    public interface Factory {
        Optional<RootAddValuesRequest> create();
    }

    public static Factory getFactory(TraceIdFactory traceIdFactory) {
        return () ->
                RootAddValuesRequest.of(traceIdFactory.createTraceId());
    }

    private static Optional<RootAddValuesRequest> of(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new RootAddValuesRequest(traceId));
    }
}