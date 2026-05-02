package time;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemWaitStrategyTest {

    @Test
    void testAwaitIndefinitelyBlocksUntilSignaled() throws InterruptedException {
        SystemWaitStrategy waitStrategy = new SystemWaitStrategy();
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        CountDownLatch threadReady = new CountDownLatch(1);
        AtomicBoolean unblockedSuccessfully = new AtomicBoolean(false);

        Thread waitingThread = new Thread(() -> {
            lock.lock();
            try {
                // Signal the main thread that we hold the lock and are about to wait
                threadReady.countDown();

                // This should block indefinitely until signaled
                waitStrategy.await(condition);

                // If we reach this line, the signal worked
                unblockedSuccessfully.set(true);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });

        waitingThread.start();

        // Wait for the background thread to signal it is ready
        assertTrue(threadReady.await(1, TimeUnit.SECONDS), "Thread failed to start in time");

        // Yield briefly to ensure the background thread has fully entered the WAITING state
        Thread.sleep(50);

        // Acquire the lock to send the signal
        lock.lock();
        try {
            condition.signal();
        } finally {
            lock.unlock();
        }

        // Wait for the background thread to finish its assertion and terminate
        waitingThread.join(1000);

        assertTrue(unblockedSuccessfully.get(), "The wait strategy should have unblocked after being signaled");
    }

    @Test
    void testAwaitNanosBlocksForApproximateDuration() throws InterruptedException {
        SystemWaitStrategy waitStrategy = new SystemWaitStrategy();
        ReentrantLock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        lock.lock();
        try {
            long startNanos = System.nanoTime();
            // Ask the OS to block for 50ms
            waitStrategy.await(condition, Duration.ofMillis(50));
            long elapsedNanos = System.nanoTime() - startNanos;

            // Allow a generous 10ms variance for OS thread scheduler inaccuracies
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
            assertTrue(elapsedMillis >= 40, "Wait strategy should have blocked for at least ~40ms, but was " + elapsedMillis);
        } finally {
            lock.unlock();
        }
    }
}