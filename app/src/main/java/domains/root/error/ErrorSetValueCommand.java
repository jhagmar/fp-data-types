package domains.root.error;

import fp.Result;

import java.util.Objects;

public final class ErrorSetValueCommand
        extends
        ErrorCommand<ErrorSetValueExecutionOutcome, ErrorSetValueExecutionError> {

    private final ErrorSetValueRequest request;

    public ErrorSetValueCommand(ErrorSetValueRequest request) {
        this.request = Objects.requireNonNull(request, "request must not be null");
    }

    @Override
    public String getTraceId() {
        return this.request.getTraceId();
    }

    @Override
    public Result<ErrorSetValueExecutionOutcome, ErrorSetValueExecutionError> apply(
            ErrorContext context, ErrorVolatileStatePart volatileStatePart,
            ErrorPersistentStateReader persistentStateReader) {

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ignored) {}

        return new Result.Ok<>(new ErrorSetValueExecutionOutcome(
                this.request.getTraceId(),
                this.request.getValue()
        ));
    }
}