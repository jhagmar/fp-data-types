package domains.root.antenna;

import domains.VolatileStatePart;

/**
 * The in-memory (volatile) state representation of the physical antenna domain.
 *
 * @param element0Operational {@code true} if the primary element (0) is currently operational
 * @param element1Operational {@code true} if the secondary element (1) is currently operational
 */
public record AntennaVolatileStatePart(
        boolean element0Operational,
        boolean element1Operational,
        int antennaValue
) implements VolatileStatePart<AntennaScope> {

}