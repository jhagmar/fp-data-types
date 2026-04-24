package domains;

import domains.root.RootRequest;
import domains.root.antenna.AntennaRequest;
import domains.root.error.ErrorRequest;
import execution.ConflictAware;
import execution.Traceable;

/**
 * The foundational contract for all executable commands or queries within the task execution engine.
 * <p>
 * This interface represents an intent to mutate state or trigger a task within a specific domain.
 * By extending {@link Traceable}, every request guarantees it carries correlation context,
 * allowing the engine to log and track the lifecycle of the command across asynchronous boundaries.
 * </p>
 * <p>
 * By utilizing the generic parameter {@code <S extends Scope>}, requests are strictly bound
 * to their target operational boundary at compile-time, preventing misrouting.
 * </p>
 *
 * @param <S> the specific {@link Scope} boundary this request targets
 */
public sealed abstract class Request<S extends Scope> implements ConflictAware<Request<?>>, Scoped<S>, Traceable
        permits RootRequest, AntennaRequest, ErrorRequest {

    @Override
    public boolean conflictsWith(Request<?> other) {
        return this.getScope().conflictsWith(other.getScope());
    }
}