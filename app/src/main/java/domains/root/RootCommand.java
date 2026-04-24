package domains.root;

import domains.Command;
import domains.ExecutionError;
import domains.ExecutionOutcome;
import domains.root.error.ErrorContext;
import domains.root.error.ErrorPersistentStateReader;
import domains.root.error.ErrorScope;
import domains.root.error.ErrorSetValueCommand;
import domains.root.error.ErrorVolatileStatePart;

/**
 * The domain-specific boundary for all executable commands within the Root scope.
 * <p>
 * This interface bridges the generic {@link Command} contract to the specific
 * dependencies of the Root domain (Context, VolatileState, and PersistentReader).
 * </p>
 *
 * @param <O> the specific {@link ExecutionOutcome} produced by implementations
 * @param <E> the specific {@link ExecutionError} produced by implementations
 */
public sealed abstract class RootCommand<
        O extends ExecutionOutcome<RootScope>,
        E extends ExecutionError<RootScope>
        > extends Command<RootScope, RootContext, RootVolatileStatePart, RootPersistentStateReader, O, E>
        permits RootAddValuesCommand {

    @Override
    public RootScope getScope() {
        return new RootScope();
    }

}