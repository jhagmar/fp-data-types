package domains.root.antenna;

import domains.ExecutionOutcome;

import java.util.Objects;

public record AntennaSetValueExecutionOutcome(
        String getTraceId,
        int value
) implements ExecutionOutcome<AntennaScope> {

    public AntennaSetValueExecutionOutcome {
        Objects.requireNonNull(getTraceId, "Execution outcome must retain its traceId");
        if (getTraceId.isBlank()) {
            throw new IllegalArgumentException("Execution outcome traceId cannot be blank");
        }
    }

}