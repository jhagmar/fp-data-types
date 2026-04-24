package execution;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a node within a directed acyclic graph (DAG) of task dependencies.
 * <p>
 * Manages the transition of a task from "blocked" to "ready" by tracking
 * upstream dependencies (in-degree).
 * </p>
 * <p>
 * <b>Note:</b> This class is intentionally <i>not</i> thread-safe. Thread safety
 * and synchronization must be managed by the caller (e.g., the {@code Scheduler}).
 * </p>
 *
 * @param <Payload> The type of the task payload.
 */
public class TaskDependencyNode<Payload extends ConflictAware<Payload> & Traceable>
        implements ConflictAware<TaskDependencyNode<Payload>>, Resolvable, Traceable {

    private final Payload payload;
    private final Set<TaskDependencyNode<Payload>> dependents;
    private final TaskResolutionListener<Payload> resolutionListener;
    private int inDegree;

    private TaskDependencyNode(Payload payload,
            TaskResolutionListener<Payload> resolutionListener) {
        this.payload = Objects.requireNonNull(payload, "Payload cannot be null");
        this.resolutionListener = Objects.requireNonNull(resolutionListener, "Listener cannot be null");
        this.dependents = new HashSet<>();
        this.inDegree = 0;
    }

    /**
     * Static factory method to create a new task dependency node.
     *
     * @param payload       The payload to wrap.
     * @param readyListener The listener to notify upon resolution.
     * @param <P>           The specific payload type.
     * @return A new TaskDependencyNode instance.
     */
    public static <P extends ConflictAware<P> & Traceable> TaskDependencyNode<P> create(
            P payload,
            TaskResolutionListener<P> readyListener) {
        return new TaskDependencyNode<>(payload, readyListener);
    }

    /**
     * Links a downstream node to this one. The dependent will be notified via
     * the listener once this node is resolved.
     *
     * @param dependent The downstream node dependent on this task.
     */
    public void addDependent(TaskDependencyNode<Payload> dependent) {
        Objects.requireNonNull(dependent, "Dependent node cannot be null");
        if (dependent == this) {
            throw new IllegalArgumentException("Circular dependency: node cannot depend on itself");
        }

        if (dependents.add(dependent)) {
            dependent.incrementInDegree();
        }
    }

    private void incrementInDegree() {
        this.inDegree++;
    }

    private void decrementInDegree() {
        this.inDegree--;
    }

    /**
     * Returns the current number of unresolved upstream dependencies.
     *
     * @return The current in-degree count.
     */
    public int getInDegree() {
        return inDegree;
    }

    /**
     * Returns an execution-safe handle for this node. This decouples the graph
     * management logic from the execution logic.
     *
     * @return A Task handle for execution.
     */
    public Task<Payload> getTask() {
        return new TaskHandle();
    }

    private void handleTaskResolved() {
        resolutionListener.onTaskResolved(this);
    }

    /**
     * Returns a read-only view of the downstream dependents.
     *
     * @return An unmodifiable collection of dependents.
     */
    Collection<TaskDependencyNode<Payload>> getDependents() {
        return List.copyOf(dependents);
    }

    /**
     * Marks this node as resolved, decrementing the in-degree of all downstream dependents.
     */
    @Override
    public void resolve() {
        for (TaskDependencyNode<Payload> dependent : dependents) {
            dependent.decrementInDegree();
        }
        dependents.clear();
    }

    @Override
    public boolean conflictsWith(TaskDependencyNode<Payload> other) {
        Objects.requireNonNull(other, "Other cannot be null.");
        return this.payload.conflictsWith(other.payload);
    }

    @Override
    public String getTraceId() {
        return this.payload.getTraceId();
    }

    @Override
    public String toString() {
        return String.format("Node[id=%s, inDegree=%d]", getTraceId(), inDegree);
    }

    /**
     * Inner implementation of the Task interface to encapsulate the node's
     * internal state.
     */
    private class TaskHandle implements Task<Payload> {

        @Override
        public Payload getPayload() {
            return payload;
        }

        @Override
        public void resolve() {
            TaskDependencyNode.this.handleTaskResolved();
        }

        @Override
        public String getTraceId() {
            return TaskDependencyNode.this.getTraceId();
        }
    }
}