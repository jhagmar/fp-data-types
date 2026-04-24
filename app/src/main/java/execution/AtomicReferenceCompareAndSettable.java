package execution;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A concrete implementation of {@link CompareAndSettable} backed by a standard Java {@link AtomicReference}.
 * <p>
 * This class provides the engine with hardware-level atomic operations. Because this
 * component is strictly internal to the execution engine, it enforces a "fail-fast"
 * null-hostile policy. It explicitly throws exceptions on invalid inputs to guarantee
 * the engine never accidentally manages or swaps a null global state.
 * </p>
 *
 * @param <T> the type of the immutable state object being managed
 */
public class AtomicReferenceCompareAndSettable<T> implements CompareAndSettable<T> {

    private final AtomicReference<T> reference;

    /**
     * Private constructor to enforce initialization through the factory method.
     */
    private AtomicReferenceCompareAndSettable(T initialValue) {
        this.reference = new AtomicReference<>(initialValue);
    }

    /**
     * Factory method to initialize the atomic reference.
     * <p>
     * Enforces a strict non-null invariant upon creation.
     * </p>
     *
     * @param initialValue the starting state (must not be null)
     * @param <T>          the type of the state
     * @return an {@link AtomicReferenceCompareAndSettable} containing the initial value
     * @throws NullPointerException if the initial value is null
     */
    public static <T> AtomicReferenceCompareAndSettable<T> of(T initialValue) {
        Objects.requireNonNull(initialValue, "Initial state cannot be null");
        return new AtomicReferenceCompareAndSettable<>(initialValue);
    }

    @Override
    public T get() {
        return reference.get();
    }

    @Override
    public boolean trySet(T expectedValue, T newValue) {
        Objects.requireNonNull(newValue, "Cannot swap to a null state");
        return reference.compareAndSet(expectedValue, newValue);
    }
}