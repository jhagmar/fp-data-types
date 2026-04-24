package domains.root.error;

import domains.root.RootVolatileStatePart;
import state_modelling.Lens;

/**
 * Focuses on the {@link ErrorVolatileStatePart} within the root state.
 */
public final class ErrorLens implements Lens<RootVolatileStatePart, ErrorVolatileStatePart> {

    @Override
    public ErrorVolatileStatePart get(RootVolatileStatePart root) {
        return root.error();
    }

    @Override
    public RootVolatileStatePart set(RootVolatileStatePart original, ErrorVolatileStatePart error) {
        // Creates a new Root state, preserving the existing antenna but substituting the error state
        return new RootVolatileStatePart(original.antenna(), error,
                                         original.rootValue());
    }
}