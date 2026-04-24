package domains;

import domains.root.RootContext;
import domains.root.antenna.AntennaContext;
import domains.root.error.ErrorContext;

/**
 * The foundational contract for read-only configuration and environmental data
 * associated with a specific execution scope.
 * <p>
 * In this execution engine, a clear boundary is maintained between mutable execution
 * data (State) and immutable environmental data (Context). This interface represents
 * the latter—holding configuration parameters, hardware limits, or tenant information
 * that the engine and its handlers can read during task execution but cannot modify.
 * </p>
 * <p>
 * By utilizing the generic parameter {@code <S extends Scope>}, the context is
 * strictly bound to its operational boundary at compile-time, ensuring that task
 * handlers only receive the configuration parameters relevant to their specific domain.
 * </p>
 *
 * @param <S> the specific {@link Scope} boundary this context belongs to
 */
public sealed interface Context<S extends Scope>
        permits RootContext, AntennaContext, ErrorContext {
}