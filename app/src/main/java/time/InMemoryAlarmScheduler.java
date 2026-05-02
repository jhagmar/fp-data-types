package time;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * A thread-safe implementation of {@link AlarmScheduler}.
 * <p>
 * This service uses a background thread to monitor scheduled alarms and guarantees
 * that alarms will not be triggered prior to their requested {@link Instant}.
 * Callbacks are executed asynchronously, and thread waiting is abstracted
 * to allow deterministic, high-speed unit testing.
 *
 * @param <T> The type of the event.
 */
public class InMemoryAlarmScheduler<T> implements AlarmScheduler<T>, AutoCloseable {

    private final Consumer<T> callback;
    private final Clock clock;
    private final Executor callbackExecutor;
    private final WaitStrategy waitStrategy;

    private final PriorityQueue<Alarm<T>> alarms;
    private final ReentrantLock lock;
    private final Condition alarmCondition;

    private final Thread workerThread;
    private volatile boolean running;

    private record Alarm<T>(Instant instant, T event) implements Comparable<Alarm<T>> {
        @Override
        public int compareTo(Alarm<T> o) {
            return this.instant.compareTo(o.instant);
        }
    }

    /**
     * Constructs a new InMemoryAlarmScheduler.
     *
     * @param callback         The consumer that processes triggered events.
     * @param clock            The time provider.
     * @param callbackExecutor The executor used to run the callbacks asynchronously.
     * @param threadFactory    The factory to create the internal background thread.
     * @param waitStrategy     The strategy handling thread suspension (injected for testing).
     */
    public InMemoryAlarmScheduler(
            Consumer<T> callback,
            Clock clock,
            Executor callbackExecutor,
            ThreadFactory threadFactory,
            WaitStrategy waitStrategy) {

        this.callback = Objects.requireNonNull(callback, "callback must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.callbackExecutor = Objects.requireNonNull(callbackExecutor, "callbackExecutor must not be null");
        this.waitStrategy = Objects.requireNonNull(waitStrategy, "waitStrategy must not be null");
        Objects.requireNonNull(threadFactory, "threadFactory must not be null");

        this.alarms = new PriorityQueue<>();
        this.lock = new ReentrantLock();
        this.alarmCondition = this.lock.newCondition();
        this.running = true;

        this.workerThread = threadFactory.newThread(this::alarmLoop);
        this.workerThread.start();
    }

    private void alarmLoop() {
        while (running) {
            List<T> eventsToExecute = new ArrayList<>();
            lock.lock();
            try {
                while (running) {
                    Alarm<T> nextAlarm = alarms.peek();

                    if (nextAlarm == null) {
                        // If we already collected events, break to execute them. Otherwise, wait.
                        if (!eventsToExecute.isEmpty()) break;
                        waitStrategy.await(alarmCondition);
                    } else {
                        Instant now = clock.instant();

                        if (now.isBefore(nextAlarm.instant())) {
                            // If we already collected events, break to execute them before sleeping again.
                            if (!eventsToExecute.isEmpty()) break;

                            Duration delay = Duration.between(now, nextAlarm.instant());
                            waitStrategy.await(alarmCondition, delay);
                        } else {
                            // Alarm expired! Collect it and loop again to check the next one.
                            alarms.poll();
                            eventsToExecute.add(nextAlarm.event());
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            } finally {
                lock.unlock();
            }

            // Execute the collected batch
            for (T event : eventsToExecute) {
                callbackExecutor.execute(() -> callback.accept(event));
            }
        }
    }

    @Override
    public void setAlarm(Instant instant, T event) {
        Objects.requireNonNull(instant, "instant must not be null");
        Objects.requireNonNull(event, "event must not be null");

        lock.lock();
        try {
            Alarm<T> newAlarm = new Alarm<>(instant, event);
            alarms.add(newAlarm);

            if (alarms.peek() == newAlarm) {
                alarmCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        running = false;
        workerThread.interrupt(); // Ensure it breaks out of waitStrategy immediately

        lock.lock();
        try {
            alarmCondition.signalAll();
        } finally {
            lock.unlock();
        }

        // Optional: wait for the thread to fully terminate
        try {
            workerThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}