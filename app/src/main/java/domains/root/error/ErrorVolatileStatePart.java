package domains.root.error;

import domains.VolatileStatePart;

/**
 * The in-memory (volatile) state representation of the system's error domain.
 */
public record ErrorVolatileStatePart(int errorValue) implements VolatileStatePart<ErrorScope> {

}