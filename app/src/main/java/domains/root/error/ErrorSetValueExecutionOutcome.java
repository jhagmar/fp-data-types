package domains.root.error;

import domains.ExecutionOutcome;
import domains.root.antenna.AntennaScope;

import java.util.Objects;

public record ErrorSetValueExecutionOutcome(
        String getTraceId,
        int value
) implements ExecutionOutcome<ErrorScope> {

    public ErrorSetValueExecutionOutcome {
        Objects.requireNonNull(getTraceId, "Execution outcome must retain its traceId");
        if (getTraceId.isBlank()) {
            throw new IllegalArgumentException("Execution outcome traceId cannot be blank");
        }
    }

}