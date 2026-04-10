package type_support;

/**
 * An object capable of incrementally computing a hash code over a sequence of elements.
 * <p>
 * Implementations of this interface are typically stateful and are designed to be
 * used as local variables for accumulating hash codes iteratively, avoiding the
 * need for temporary array allocations.
 */
public interface Hasher {

    /**
     * Incorporates the given object's hash code into the accumulated hash state.
     *
     * @param o the object to hash; must not be null
     * @return this {@code Hasher} instance, allowing for method chaining
     * @throws NullPointerException if the specified object is null
     */
    Hasher hash(Object o);

    /**
     * Returns the final accumulated hash code computed from all objects
     * passed to {@link #hash(Object)}.
     *
     * @return the computed hash code
     */
    int getHashCode();
}