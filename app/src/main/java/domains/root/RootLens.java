package domains.root;

import domains.VolatileAppState;
import state_modelling.Lens;

/**
 * Focuses on the {@link RootVolatileStatePart} within the global application state.
 */
public final class RootLens implements Lens<VolatileAppState, RootVolatileStatePart> {

    @Override
    public RootVolatileStatePart get(VolatileAppState state) {
        return state.root();
    }

    @Override
    public VolatileAppState set(VolatileAppState original, RootVolatileStatePart root) {
        return new VolatileAppState(root);
    }
}