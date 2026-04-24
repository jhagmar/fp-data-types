package domains;

import domains.root.RootLens;
import domains.root.RootVolatileStatePart;
import domains.root.antenna.AntennaLens;
import domains.root.antenna.AntennaVolatileStatePart;
import domains.root.error.ErrorLens;
import domains.root.error.ErrorVolatileStatePart;
import state_modelling.Lens;

/**
 * A centralized registry of composed lenses for traversing and mutating the global application state.
 * <p>
 * This utility class statically caches the composite lenses required by the execution engine
 * to perform deep, thread-safe, and immutable updates to specific domain states
 * (like Antenna or Error). By using these pre-composed lenses, the engine can interact
 * directly with deep state fragments without needing to manually traverse or rebuild
 * the entire state tree.
 * </p>
 */
public final class VolatileAppStateLenses {

    /**
     * Prevents instantiation of this static utility class.
     */
    private VolatileAppStateLenses() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * A lens focusing on the top-level Root domain state from the global application state.
     */
    public static final Lens<VolatileAppState, RootVolatileStatePart> ROOT = new RootLens();

    /**
     * A composed lens focusing specifically on the Antenna domain state.
     * <p>
     * Enables direct, functional read/write access to the {@link AntennaVolatileStatePart}
     * starting from the top-level {@link VolatileAppState}.
     * </p>
     */
    public static final Lens<VolatileAppState, AntennaVolatileStatePart> ANTENNA =
            ROOT.andThen(new AntennaLens());

    /**
     * A composed lens focusing specifically on the Error domain state.
     * <p>
     * Enables direct, functional read/write access to the {@link ErrorVolatileStatePart}
     * starting from the top-level {@link VolatileAppState}.
     * </p>
     */
    public static final Lens<VolatileAppState, ErrorVolatileStatePart> ERROR =
            ROOT.andThen(new ErrorLens());

}