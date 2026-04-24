package state_modelling;

/**
 * A functional Lens for immutable state traversal and mutation.
 * <p>
 * A Lens operates on a specific {@code Whole} (W) to view or update a specific {@code Part} (P).
 * By keeping the interface purely generic, Lenses can be composed together to drill down
 * through deeply nested immutable data structures without hardcoding paths.
 * </p>
 *
 * @param <W> The "Whole" (the parent data structure)
 * @param <P> The "Part" (the specific component being focused on)
 */
public interface Lens<W, P> {

    /**
     * Extracts the part from the whole.
     */
    P get(W whole);

    /**
     * Creates a new instance of the whole with an updated part.
     */
    W set(W whole, P part);

    /**
     * Composes this lens with another lens, allowing deep, nested updates.
     * <p>
     * For example, composing {@code Lens<App, Root>} with {@code Lens<Root, Antenna>}
     * yields a seamless {@code Lens<App, Antenna>}.
     * </p>
     *
     * @param next the next lens to apply to the part extracted by this lens
     * @param <C>  the "Child" part targeted by the next lens
     * @return a new composite Lens from the original Whole down to the Child part
     */
    default <C> Lens<W, C> andThen(Lens<P, C> next) {
        return new Lens<>() {
            @Override
            public C get(W whole) {
                // Get the Part from the Whole, then get the Child from the Part
                return next.get(Lens.this.get(whole));
            }

            @Override
            public W set(W whole, C childPart) {
                // Set the Child into the Part, then set the updated Part into the Whole
                P updatedPart = next.set(Lens.this.get(whole), childPart);
                return Lens.this.set(whole, updatedPart);
            }
        };
    }
}