package domains;

import domains.root.RootVolatileStatePart;

/**
 * The absolute top-level container for the application's in-memory state.
 * <p>
 * {@code VolatileAppState} wraps the {@link RootVolatileStatePart}, serving as the
 * single immutable source of truth for the execution engine at any given millisecond.
 * Because it is an immutable record, any state transition across the entire system
 * results in a new instance of this object, enabling lock-free, thread-safe state
 * management.
 * </p>
 *
 * @param root the aggregate root containing all sub-domain states (e.g., Antenna, Error)
 */
public record VolatileAppState(RootVolatileStatePart root) {

}