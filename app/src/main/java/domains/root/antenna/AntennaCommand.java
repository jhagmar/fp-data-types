package domains.root.antenna;

import domains.Command;
import domains.ExecutionError;
import domains.ExecutionOutcome;

/**
 * The domain-specific boundary for all executable commands within the Antenna scope.
 * <p>
 * This interface bridges the generic {@link Command} contract to the specific
 * dependencies of the Antenna domain (Context, VolatileState, and PersistentReader).
 * </p>
 *
 * @param <O> the specific {@link ExecutionOutcome} produced by implementations
 * @param <E> the specific {@link ExecutionError} produced by implementations
 */
public sealed abstract class AntennaCommand<
        O extends ExecutionOutcome<AntennaScope>,
        E extends ExecutionError<AntennaScope>
        > extends Command<AntennaScope, AntennaContext, AntennaVolatileStatePart, AntennaPersistentStateReader, O, E>
        permits AntennaElementStatusCommand, AntennaSetValueCommand {

    @Override
    public AntennaScope getScope() {
        return new AntennaScope();
    }

}