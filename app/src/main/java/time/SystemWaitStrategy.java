package time;

import java.time.Duration;
import java.util.concurrent.locks.Condition;

/**
 * Production implementation of {@link WaitStrategy} that utilizes the
 * standard system clock and OS thread scheduler.
 */
public class SystemWaitStrategy implements WaitStrategy {

    @Override
    public void await(Condition condition) throws InterruptedException {
        condition.await();
    }

    @Override
    public void await(Condition condition, Duration delay) throws InterruptedException {
        condition.awaitNanos(delay.toNanos());
    }
}