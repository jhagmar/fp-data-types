package domains.root.antenna;

import domains.root.RootVolatileStatePart;
import state_modelling.Lens;

/**
 * Focuses on the {@link AntennaVolatileStatePart} within the root state.
 */
public final class AntennaLens implements Lens<RootVolatileStatePart, AntennaVolatileStatePart> {

    @Override
    public AntennaVolatileStatePart get(RootVolatileStatePart root) {
        return root.antenna();
    }

    @Override
    public RootVolatileStatePart set(RootVolatileStatePart original, AntennaVolatileStatePart antenna) {
        // Creates a new Root state, substituting the antenna but preserving the existing error state
        return new RootVolatileStatePart(antenna, original.error(),
                                         original.rootValue());
    }
}