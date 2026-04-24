package domains;

import domains.root.RootCommand;
import domains.root.antenna.AntennaCommand;
import domains.root.error.ErrorCommand;
import execution.ConflictAware;
import execution.Traceable;
import fp.Result;

/**
 * The core behavioral contract for executing business logic within a specific
 * domain boundary.
 * <p>
 * This interface represents a pure functional transformation:
 * {@code (Context, State, Reader) -> Result<Outcome, Error>}.
 * </p>
 * <p>
 * To prevent "Outcome Bleeding"—where a command accidentally returns an outcome
 * that is valid for the domain but invalid for the specific task—the specific
 * {@code Outcome} and {@code Error} types are bound at the command level.
 * </p>
 *
 * @param <S>
 *         the operational {@link Scope} boundary
 * @param <C>
 *         the read-only {@link Context} for this scope
 * @param <VSP>
 *         the in-memory {@link VolatileStatePart} snapshot
 * @param <PSR>
 *         the {@link PersistentStateReader} for this domain
 * @param <O>
 *         the specific {@link ExecutionOutcome} this command is permitted to
 *         produce
 * @param <E>
 *         the specific {@link ExecutionError} this command is permitted to
 *         produce
 */
public sealed abstract class Command<
        S extends Scope,
        C extends Context<S>,
        VSP extends VolatileStatePart<S>,
        PSR extends PersistentStateReader<S>,
        O extends ExecutionOutcome<S>,
        E extends ExecutionError<S>
        > implements
        ConflictAware<Command<? extends Scope, ? extends Context<?>, ? extends VolatileStatePart<?>, ? extends PersistentStateReader<?>, ? extends ExecutionOutcome<?>, ? extends ExecutionError<?>>>,
        Scoped<S>, Traceable permits RootCommand, AntennaCommand, ErrorCommand {

    /**
     * Executes the domain logic as a pure, side-effect-free function.
     * <p>
     * Instead of throwing exceptions, business rule violations are captured in
     * the {@code E} (Error) side of the {@link Result} monad. On success, the
     * specific {@code O} (Outcome) is returned to be applied to the global
     * state.
     * </p>
     *
     * @param context
     *         the immutable configuration/environment
     * @param volatileStatePart
     *         the current immutable snapshot of the domain state
     * @param persistentStateReader
     *         the repository port for historical data
     * @return a monad containing either the specific success outcome or the
     *         specific error
     */
    public abstract Result<O, E> apply(C context, VSP volatileStatePart,
            PSR persistentStateReader);

    @Override
    public boolean conflictsWith(Command<?, ?, ?, ?, ?, ?> other) {
        return this.getScope().conflictsWith(other.getScope());
    }
}