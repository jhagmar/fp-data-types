package domains.root.error;

import domains.ExecutionError;

public record ErrorSetValueExecutionError() implements ExecutionError<ErrorScope> {
}