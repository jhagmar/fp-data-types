package domains.root.antenna;

import domains.Request;

/**
 * The domain-specific boundary for all requests targeting the antenna domain.
 * <p>
 * This sealed interface groups all valid commands and queries that can be routed to
 * the {@link AntennaScope}.
 * </p>
 */
public sealed abstract class AntennaRequest extends Request<AntennaScope>
        permits AntennaElementStatusRequest, AntennaSetValueRequest {

    @Override
    public AntennaScope getScope() {
        return new AntennaScope();
    }

}