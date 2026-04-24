package domains.root.antenna;

import domains.Command;
import fp.Result;
import java.util.Objects;

/**
 * A concrete command responsible for updating the status of a specific antenna element.
 * <p>
 * This class implements the pure business logic required to transition the antenna state.
 * It strictly returns a {@link Result} containing either a success outcome or a
 * specific element-status error, ensuring no side effects occur during the calculation phase.
 * </p>
 */
public final class AntennaElementStatusCommand
        extends AntennaCommand<AntennaElementStatusExecutionOutcome, AntennaElementStatusExecutionError> {

    private final AntennaElementStatusRequest request;

    /**
     * Constructs the command with the necessary request data and execution context.
     * @param request the validated request from the DMZ
     */
    public AntennaElementStatusCommand(AntennaElementStatusRequest request) {
        this.request = Objects.requireNonNull(request, "request must not be null");
    }

    @Override
    public Result<AntennaElementStatusExecutionOutcome, AntennaElementStatusExecutionError> apply(
            AntennaContext ignoredContext,
            AntennaVolatileStatePart volatileStatePart,
            AntennaPersistentStateReader persistentStateReader) {

        // Note: In a production system, we might use the persistentStateReader here
        // to e.g. check historical element health before allowing an 'operational' status.

        final boolean element0Operational = request.getElement() == 0
                ? request.isOperational()
                : volatileStatePart.element0Operational();

        final boolean element1Operational = request.getElement() == 1
                ? request.isOperational()
                : volatileStatePart.element1Operational();

        return new Result.Ok<>(new AntennaElementStatusExecutionOutcome(
                this.request.getTraceId(),
                element0Operational,
                element1Operational
        ));
    }

    @Override
    public String getTraceId() {
        return this.request.getTraceId();
    }
}