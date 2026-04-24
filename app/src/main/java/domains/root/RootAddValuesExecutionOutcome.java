package domains.root;

import domains.ExecutionOutcome;

import java.util.Objects;

public record RootAddValuesExecutionOutcome(
        String getTraceId,
        int value
) implements ExecutionOutcome<RootScope> {

    public RootAddValuesExecutionOutcome {
        Objects.requireNonNull(getTraceId, "Execution outcome must retain its traceId");
        if (getTraceId.isBlank()) {
            throw new IllegalArgumentException("Execution outcome traceId cannot be blank");
        }
    }

}