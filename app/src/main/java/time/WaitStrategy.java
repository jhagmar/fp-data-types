package time;

import java.time.Duration;
import java.util.concurrent.locks.Condition;

/**
 * Abstracts the mechanism of waiting on a Condition.
 * This prevents tight coupling to OS-level thread scheduling, allowing
 * unit tests to use virtual time without locking up the test suite.
 */
public interface WaitStrategy {

    /**
     * Waits indefinitely until the condition is signaled.
     * Must be called while holding the Lock associated with the Condition.
     */
    void await(Condition condition) throws InterruptedException;

    /**
     * Waits up to the specified duration, or until the condition is signaled.
     * Must be called while holding the Lock associated with the Condition.
     */
    void await(Condition condition, Duration delay) throws InterruptedException;
}
