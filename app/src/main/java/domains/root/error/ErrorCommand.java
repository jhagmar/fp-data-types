package domains.root.error;

import domains.Command;
import domains.ExecutionError;
import domains.ExecutionOutcome;
import domains.root.antenna.AntennaContext;
import domains.root.antenna.AntennaElementStatusCommand;
import domains.root.antenna.AntennaPersistentStateReader;
import domains.root.antenna.AntennaScope;
import domains.root.antenna.AntennaSetValueCommand;
import domains.root.antenna.AntennaVolatileStatePart;

/**
 * The domain-specific boundary for all executable commands within the Error scope.
 * <p>
 * This interface bridges the generic {@link Command} contract to the specific
 * dependencies of the Error domain (Context, VolatileState, and PersistentReader).
 * </p>
 *
 * @param <O> the specific {@link ExecutionOutcome} produced by implementations
 * @param <E> the specific {@link ExecutionError} produced by implementations
 */
public sealed abstract class ErrorCommand<
        O extends ExecutionOutcome<ErrorScope>,
        E extends ExecutionError<ErrorScope>
        > extends Command<ErrorScope, ErrorContext, ErrorVolatileStatePart, ErrorPersistentStateReader, O, E>
        permits ErrorSetValueCommand {

    @Override
    public ErrorScope getScope() {
        return new ErrorScope();
    }

}