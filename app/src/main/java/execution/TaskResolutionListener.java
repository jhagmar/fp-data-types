package execution;

/**
 * A listener that is notified when a task has been resolved.
 *
 * @param <Payload> The type of the task payload.
 */
public interface TaskResolutionListener<Payload extends ConflictAware<Payload> & Traceable> {

    /**
     * Invoked when the task corresponding to a {@code TaskDependencyNode}
     * has been resolved.
     *
     * @param node The node whose task has been resolved.
     */
    void onTaskResolved(TaskDependencyNode<Payload> node);
}