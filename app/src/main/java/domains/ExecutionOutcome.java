package domains;

import execution.Traceable;

/**
 * The foundational contract representing the result of a processed request or task execution.
 * <p>
 * While a {@code Request} models the intent to do something, an {@code ExecutionOutcome}
 * models the finalized, verified result of that action. By extending {@link Traceable},
 * every outcome carries the correlation ID of its originating request, enabling
 * full lifecycle tracking from the DMZ down to state persistence.
 * </p>
 * <p>
 * Utilizing the generic parameter {@code <S extends Scope>}, the engine's state-mutation
 * layer guarantees it applies the correct outcome payload to the corresponding operational boundary.
 * </p>
 *
 * @param <S> the specific {@link Scope} boundary this outcome belongs to
 */
public interface ExecutionOutcome<S extends Scope> extends Traceable {

}