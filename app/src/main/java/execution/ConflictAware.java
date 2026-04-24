package execution;

/**
 * Represents a task that can evaluate whether it can run concurrently with another task.
 * * <p>Tasks that conflict must not be executed in parallel. The scheduler uses this
 * interface to build a dependency graph, ensuring that conflicting tasks are
 * executed sequentially.
 *
 * @param <T> The specific type of the task implementing this interface.
 */
public interface ConflictAware<T extends ConflictAware<T>> {

    /**
     * Determines if this task conflicts with another task.
     *
     * @param other The other task to compare against.
     * @return {@code true} if the tasks conflict and must run sequentially;
     * {@code false} if they can safely execute in parallel.
     */
    boolean conflictsWith(T other);
}