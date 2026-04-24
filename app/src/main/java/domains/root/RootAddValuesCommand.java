package domains.root;

import domains.root.error.ErrorCommand;
import domains.root.error.ErrorContext;
import domains.root.error.ErrorPersistentStateReader;
import domains.root.error.ErrorVolatileStatePart;
import fp.Result;

import java.util.Objects;

public final class RootAddValuesCommand
        extends
        RootCommand<RootAddValuesExecutionOutcome, RootAddValuesExecutionError> {

    private final RootAddValuesRequest request;

    public RootAddValuesCommand(RootAddValuesRequest request) {
        this.request = Objects.requireNonNull(request, "request must not be null");
    }

    @Override
    public String getTraceId() {
        return this.request.getTraceId();
    }

    @Override
    public Result<RootAddValuesExecutionOutcome, RootAddValuesExecutionError> apply(
            RootContext context, RootVolatileStatePart volatileStatePart,
            RootPersistentStateReader persistentStateReader) {

        try {
            Thread.sleep(1000);
        }
        catch (InterruptedException ignored) {}

        return Result.ok(new RootAddValuesExecutionOutcome(
                getTraceId(),
                volatileStatePart.antenna().antennaValue() + volatileStatePart.error().errorValue()
        ));
    }
}