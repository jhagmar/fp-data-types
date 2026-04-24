package domains;

import domains.root.RootVolatileStatePart;
import domains.root.antenna.AntennaVolatileStatePart;
import domains.root.error.ErrorVolatileStatePart;

/**
 * Represents a fragment of the in-memory (volatile) state associated with a specific execution scope.
 * <p>
 * In this domain-driven execution engine, state is explicitly separated into volatile (cached/in-memory)
 * and persistent components. This interface acts as the foundation for the volatile side, allowing
 * the engine to perform fast, high-throughput state reads and mutations during task execution
 * without immediately requiring persistent I/O operations.
 * </p>
 * <p>
 * By utilizing a generic type parameter {@code <S extends Scope>}, this interface establishes a
 * strict compile-time guarantee that a specific state fragment strictly belongs to, and is managed
 * within, its corresponding execution boundary.
 * </p>
 *
 * @param <S> the specific {@link Scope} boundary this volatile state is bound to
 */
public sealed interface VolatileStatePart<S extends Scope>
        permits RootVolatileStatePart, AntennaVolatileStatePart,
        ErrorVolatileStatePart {
}