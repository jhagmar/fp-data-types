package domains.root;

import domains.Request;
import domains.root.error.ErrorScope;
import domains.root.error.ErrorSetValueRequest;

/**
 * The domain-specific boundary for all requests targeting the root domain.
 * <p>
 * This sealed interface groups all valid commands and queries that can be routed to
 * the {@link RootScope}.
 * </p>
 */
public sealed abstract class RootRequest extends Request<RootScope>
        permits RootAddValuesRequest {

    @Override
    public RootScope getScope() {
        return new RootScope();
    }

}