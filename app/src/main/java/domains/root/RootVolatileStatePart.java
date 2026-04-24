package domains.root;

import domains.VolatileStatePart;
import domains.root.antenna.AntennaVolatileStatePart;
import domains.root.error.ErrorVolatileStatePart;

/**
 * The aggregate root of the in-memory (volatile) state for a given state tree.
 * <p>
 * {@code RootVolatileStatePart} serves as the top-level container for all volatile state
 * associated with a {@link RootScope}. It structurally composes the state of all
 * underlying subsystems (like antennas and errors), providing the execution engine
 * with a comprehensive, immutable snapshot of the current execution context.
 * </p>
 * <p>
 * Because it is an immutable record, state transitions in the engine should be
 * handled by deriving a new instance of this record with updated child components,
 * ensuring thread-safe reads for concurrent engine processes.
 * </p>
 *
 * @param antenna the volatile state component representing the antenna
 * @param error   the volatile state component representing the current error registry
 */
public record RootVolatileStatePart(
        AntennaVolatileStatePart antenna,
        ErrorVolatileStatePart error,
        int rootValue
) implements VolatileStatePart<RootScope> {

}