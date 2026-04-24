package domains.root.error;

import domains.Request;
import domains.root.antenna.AntennaElementStatusRequest;
import domains.root.antenna.AntennaScope;
import domains.root.antenna.AntennaSetValueRequest;

/**
 * The domain-specific boundary for all requests targeting the error domain.
 * <p>
 * This sealed interface groups all valid commands and queries that can be routed to
 * the {@link ErrorScope}.
 * </p>
 */
public sealed abstract class ErrorRequest extends Request<ErrorScope>
        permits ErrorSetValueRequest {

    @Override
    public ErrorScope getScope() {
        return new ErrorScope();
    }

}