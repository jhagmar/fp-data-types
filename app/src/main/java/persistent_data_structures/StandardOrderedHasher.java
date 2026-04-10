package persistent_data_structures;

import java.util.Objects;

/**
 * A standard implementation of {@link Hasher} that computes hash codes
 * using the traditional Java Collections formula: {@code 31 * currentHash + elementHash}.
 * <p>
 * This implementation is designed to exactly match the hashing algorithm used by 
 * {@link java.util.List#hashCode()}.
 * <p>
 * Note: This class is mutable and not thread-safe. Instances 
 * should not be shared across threads and are best utilized as local variables.
 */
public final class StandardOrderedHasher implements Hasher {

    private int hashCode;

    /**
     * Constructs a new {@code StandardOrderedHasher} initialized with the standard 
     * Java collections starting value of 1.
     */
    public StandardOrderedHasher() {
        this.hashCode = 1;
    }

    @Override
    public Hasher hash(Object o) {
        // Fail-fast with a standard NullPointerException if null is passed
        Objects.requireNonNull(o, "StandardOrderedHasher does not permit null elements");
        
        // 31 is the standard prime multiplier used throughout the JDK
        this.hashCode = 31 * this.hashCode + o.hashCode();
        
        return this;
    }

    @Override
    public int getHashCode() {
        return this.hashCode;
    }
}