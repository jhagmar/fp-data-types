package time;

import java.time.Instant;

/**
 * A service responsible for scheduling and triggering events at a specific instant in time.
 *
 * @param <T> The type of the event to be delivered when the alarm triggers.
 */
public interface AlarmScheduler<T> {

    /**
     * Schedules a event to be triggered at the specified time.
     *
     * @param instant The exact time the alarm should trigger.
     * @param event The event to pass to the callback.
     */
    void setAlarm(Instant instant, T event);
}