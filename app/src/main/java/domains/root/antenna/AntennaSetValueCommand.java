package domains.root.antenna;

import fp.Result;

import java.util.Objects;

public final class AntennaSetValueCommand
        extends AntennaCommand<AntennaSetValueExecutionOutcome, AntennaSetValueExecutionError> {

    private final AntennaSetValueRequest request;

    public AntennaSetValueCommand(AntennaSetValueRequest request) {
        this.request = Objects.requireNonNull(request, "request must not be null");
    }

    @Override
    public Result<AntennaSetValueExecutionOutcome, AntennaSetValueExecutionError> apply(
            AntennaContext ignoredContext,
            AntennaVolatileStatePart volatileStatePart,
            AntennaPersistentStateReader persistentStateReader) {

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ignored) {}

        return new Result.Ok<>(new AntennaSetValueExecutionOutcome(
                this.request.getTraceId(),
                this.request.getValue()
        ));
    }

    @Override
    public String getTraceId() {
        return this.request.getTraceId();
    }
}