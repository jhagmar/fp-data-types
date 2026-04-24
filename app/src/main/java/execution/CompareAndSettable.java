package execution;

/**
 * A generic contract for lock-free, atomic state mutations using the Compare-And-Swap (CAS) pattern.
 * <p>
 * This interface is critical for the execution engine's throughput. Instead of
 * blocking threads with traditional locks, the engine can read a state, compute
 * the next state, and use {@code trySet} to apply it. If the state was modified
 * concurrently by another thread, the swap safely fails, allowing the engine to retry.
 * </p>
 *
 * @param <T> the type of the value being managed (typically the global state)
 */
public interface CompareAndSettable<T> {

    /**
     * Retrieves the current value.
     *
     * @return the current state
     */
    T get();

    /**
     * Atomically sets the value to {@code newValue} if the current value equals {@code expectedValue}.
     *
     * @param expectedValue the value that the caller assumes is currently held
     * @param newValue      the new value to apply
     * @return {@code true} if the swap was successful; {@code false} if the current value
     * did not match the expected value (indicating a concurrent modification)
     */
    boolean trySet(T expectedValue, T newValue);
}