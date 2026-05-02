package time;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryAlarmSchedulerTest {

    private MutableClock virtualClock;
    private List<String> executedEvents;
    private Executor directExecutor;
    private WaitStrategy testWaitStrategy;
    private ThreadFactory daemonThreadFactory;

    private InMemoryAlarmScheduler<String> scheduler;

    @BeforeEach
    void setUp() {
        // Start virtual time at a fixed point
        virtualClock = new MutableClock(Instant.parse("2026-05-02T10:00:00Z"));
        executedEvents = Collections.synchronizedList(new ArrayList<>());

        // Execute callbacks synchronously on the worker thread for predictable test assertions
        directExecutor = Runnable::run;

        // Ensure test threads don't block the JVM from exiting if a test fails
        daemonThreadFactory = runnable -> {
            Thread t = new Thread(runnable);
            t.setDaemon(true);
            return t;
        };

        // The "Magic" testing strategy: Ignore requested delays and poll every 5ms in real-time.
        // This allows virtual time to dictate execution instantly.
        testWaitStrategy = new WaitStrategy() {
            @Override
            public void await(Condition condition) throws InterruptedException {
                condition.await(5, TimeUnit.MILLISECONDS);
            }

            @Override
            public void await(Condition condition, Duration delay) throws InterruptedException {
                condition.await(5, TimeUnit.MILLISECONDS);
            }
        };

        scheduler = new InMemoryAlarmScheduler<>(
                executedEvents::add,
                virtualClock,
                directExecutor,
                daemonThreadFactory,
                testWaitStrategy
        );
    }

    @AfterEach
    void tearDown() {
        if (scheduler != null) {
            scheduler.close();
        }
    }

    @Test
    void testConstructorValidatesNulls() {
        assertThrows(NullPointerException.class, () ->
                new InMemoryAlarmScheduler<String>(null, virtualClock, directExecutor, daemonThreadFactory, testWaitStrategy));

        assertThrows(NullPointerException.class, () ->
                new InMemoryAlarmScheduler<String>(event -> executedEvents.add(event), null, directExecutor, daemonThreadFactory, testWaitStrategy));

        assertThrows(NullPointerException.class, () ->
                new InMemoryAlarmScheduler<String>(event -> executedEvents.add(event), virtualClock, null, daemonThreadFactory, testWaitStrategy));

        assertThrows(NullPointerException.class, () ->
                new InMemoryAlarmScheduler<String>(event -> executedEvents.add(event), virtualClock, directExecutor, null, testWaitStrategy));

        assertThrows(NullPointerException.class, () ->
                new InMemoryAlarmScheduler<String>(event -> executedEvents.add(event), virtualClock, directExecutor, daemonThreadFactory, null));
    }

    @Test
    void testAlarmScheduledInThePastFiresImmediately() throws InterruptedException {
        Instant past = virtualClock.instant().minusSeconds(10);

        scheduler.setAlarm(past, "PastEvent");
        waitForEvents(1);

        assertEquals(1, executedEvents.size());
        assertEquals("PastEvent", executedEvents.getFirst());
    }

    @Test
    void testAlarmFiresOnlyWhenVirtualTimeAdvances() throws InterruptedException {
        Instant future = virtualClock.instant().plus(Duration.ofHours(1));

        scheduler.setAlarm(future, "FutureEvent");

        // Wait briefly in real time to prove it doesn't fire early
        Thread.sleep(50);
        assertTrue(executedEvents.isEmpty(), "Alarm should not fire before time advances");

        // Advance virtual time past the trigger point
        virtualClock.advance(Duration.ofMinutes(61));
        waitForEvents(1);

        assertEquals(1, executedEvents.size());
        assertEquals("FutureEvent", executedEvents.getFirst());
    }

    @Test
    void testMultipleAlarmsBatchExecutionCorrectly() throws InterruptedException {
        Instant targetTime = virtualClock.instant().plusSeconds(30);

        scheduler.setAlarm(targetTime, "Event1");
        scheduler.setAlarm(targetTime, "Event2");
        scheduler.setAlarm(targetTime, "Event3");

        virtualClock.advance(Duration.ofSeconds(31));
        waitForEvents(3);

        assertEquals(3, executedEvents.size());
        assertTrue(executedEvents.containsAll(List.of("Event1", "Event2", "Event3")));
    }

    @Test
    void testAlarmsTriggerInChronologicalOrder() throws InterruptedException {
        Instant now = virtualClock.instant();

        // Schedule out of order
        scheduler.setAlarm(now.plusSeconds(30), "Third");
        scheduler.setAlarm(now.plusSeconds(10), "First");
        scheduler.setAlarm(now.plusSeconds(20), "Second");

        virtualClock.advance(Duration.ofSeconds(15));
        waitForEvents(1);
        assertEquals(List.of("First"), executedEvents);

        virtualClock.advance(Duration.ofSeconds(10)); // Total 25s
        waitForEvents(2);
        assertEquals(List.of("First", "Second"), executedEvents);

        virtualClock.advance(Duration.ofSeconds(10)); // Total 35s
        waitForEvents(3);
        assertEquals(List.of("First", "Second", "Third"), executedEvents);
    }

    @Test
    void testCloseShutsDownGracefully() throws InterruptedException {
        Instant future = virtualClock.instant().plusSeconds(60);
        scheduler.setAlarm(future, "NeverFires");

        scheduler.close();

        // Time advances, but the scheduler is dead
        virtualClock.advance(Duration.ofSeconds(100));
        Thread.sleep(50); // Give it a moment to mistakenly fire if it's broken

        assertTrue(executedEvents.isEmpty(), "Events should not fire after scheduler is closed");
    }

    // --- Test Utilities ---

    /**
     * Blocks the test thread until the expected number of events have fired,
     * or times out to prevent test deadlocks.
     */
    private void waitForEvents(int expectedCount) throws InterruptedException {
        int attempts = 0;
        while (executedEvents.size() < expectedCount && attempts < 200) {
            Thread.sleep(5);
            attempts++;
        }
        if (executedEvents.size() < expectedCount) {
            fail("Timed out waiting for " + expectedCount + " events to fire. Only fired " + executedEvents.size());
        }
    }

    /**
     * A controllable Clock implementation for deterministic time testing.
     */
    private static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zoneId = ZoneId.of("UTC");

        public MutableClock(Instant initialInstant) {
            this.instant = initialInstant;
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void advance(Duration duration) {
            this.instant = this.instant.plus(duration);
        }
    }

    @Test
    void testAlarmLoopRestoresInterruptStatus() throws InterruptedException {
        AtomicBoolean threadDiedWithInterruptFlag = new AtomicBoolean(false);

        // A mock strategy that forces the InterruptedException immediately
        WaitStrategy throwingStrategy = new WaitStrategy() {
            @Override
            public void await(Condition condition) throws InterruptedException {
                throw new InterruptedException("Simulated interrupt");
            }

            @Override
            public void await(Condition condition, Duration delay) throws InterruptedException {
                throw new InterruptedException("Simulated interrupt");
            }
        };

        // A custom thread factory to intercept the thread just before it terminates
        ThreadFactory threadFactory = runnable -> new Thread(() -> {
            runnable.run();
            // Capture the interrupt status right as the thread exits the alarmLoop
            threadDiedWithInterruptFlag.set(Thread.currentThread().isInterrupted());
        });

        InMemoryAlarmScheduler<String> customScheduler = new InMemoryAlarmScheduler<String>(
                event -> {}, virtualClock, directExecutor, threadFactory, throwingStrategy);

        // Give the background thread a few milliseconds to start, throw, and terminate
        Thread.sleep(50);

        // Verify the catch block successfully called Thread.currentThread().interrupt()
        assertTrue(threadDiedWithInterruptFlag.get(), "alarmLoop catch block should restore the interrupt status");
    }

    @Test
    void testCloseRestoresInterruptStatusDuringJoin() {
        // Set the interrupt flag on the CURRENT (Main test) thread.
        Thread.currentThread().interrupt();

        // Calling close() will hit workerThread.join(), which immediately throws
        // because the thread is already interrupted, clearing the flag in the process.
        scheduler.close();

        // Verify the catch block inside close() caught it and re-applied the interrupt flag
        assertTrue(Thread.currentThread().isInterrupted(), "close() catch block should restore the interrupt status");

        // IMPORTANT: Clear the interrupt status so it doesn't leak into JUnit or subsequent tests!
        Thread.interrupted();
    }
}