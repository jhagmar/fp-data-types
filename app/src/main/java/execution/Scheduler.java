package execution;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Manages the dependency graph and coordinates the dispatching of tasks.
 * <p>
 * This class acts as the primary synchronization monitor for the execution engine.
 * Methods mutating the underlying task graph are synchronized to ensure thread safety
 * without requiring fine-grained locks on individual {@link TaskDependencyNode} instances.
 * </p>
 *
 * @param <Payload> The type of the task payload.
 */
public class Scheduler<Payload extends ConflictAware<Payload> & Traceable>
        implements TaskResolutionListener<Payload> {

    private static final System.Logger LOGGER = System.getLogger(Scheduler.class.getName());

    private final Set<TaskDependencyNode<Payload>> unresolvedNodes;
    private final WorkQueue<Payload> readyQueue;

    private Scheduler(WorkQueue<Payload> readyQueue) {
        this.unresolvedNodes = new HashSet<>();
        this.readyQueue = readyQueue;
    }

    /**
     * Creates a new Scheduler instance.
     *
     * @param readyQueue The queue to which ready tasks will be submitted.
     * @param <Payload>  The type of the task payload.
     * @return A newly constructed Scheduler.
     */
    public static <Payload extends ConflictAware<Payload> & Traceable> Scheduler<Payload> create(WorkQueue<Payload> readyQueue) { // Fixed to public static with generic type
        Objects.requireNonNull(readyQueue, "readyQueue must not be null.");
        return new Scheduler<>(readyQueue);
    }

    private void tryEnqueue(Task<Payload> task) {
        if (!readyQueue.tryEnqueue(task)) {
            LOGGER.log(System.Logger.Level.ERROR, "Task dropped due to backpressure, trace ID {0}", task.getTraceId());
        } else {
            LOGGER.log(System.Logger.Level.DEBUG, "Task enqueued, trace ID: {0}", task.getTraceId());
        }
    }

    /**
     * Submits a new payload to the scheduler. It will be evaluated for conflicts
     * against currently unresolved tasks and enqueued if unconstrained.
     *
     * @param payload The payload to schedule.
     */
    public synchronized void add(Payload payload) {
        Objects.requireNonNull(payload, "Payload must not be null.");

        LOGGER.log(System.Logger.Level.DEBUG, "Adding task, trace ID: {0}", payload.getTraceId());

        TaskDependencyNode<Payload> node = TaskDependencyNode.create(payload, this);
        boolean nodeIsConflictFree = true;

        for (TaskDependencyNode<Payload> unresolvedNode : unresolvedNodes) {
            if (node.conflictsWith(unresolvedNode)) {
                nodeIsConflictFree = false;
                unresolvedNode.addDependent(node);
                LOGGER.log(System.Logger.Level.DEBUG, "Adding dependency edge: {0} -> {1}", unresolvedNode.getTraceId(), node.getTraceId());
            }
        }

        unresolvedNodes.add(node);

        if (nodeIsConflictFree) {
            this.tryEnqueue(node.getTask());
        }
    }

    /**
     * Handles the notification that a task has completed execution.
     * This will resolve the node and potentially free up downstream dependents.
     *
     * @param node The node that has resolved.
     */
    @Override
    public synchronized void onTaskResolved(TaskDependencyNode<Payload> node) {
        LOGGER.log(System.Logger.Level.DEBUG, "Task resolved, trace ID {0}", node.getTraceId());

        Collection<TaskDependencyNode<Payload>> dependents = node.getDependents();
        node.resolve();
        for (TaskDependencyNode<Payload> dependent : dependents) {
            if (dependent.getInDegree() == 0) {
                LOGGER.log(System.Logger.Level.DEBUG,"Task ready for processing, trace ID {0}", dependent.getTraceId());
                tryEnqueue(dependent.getTask());
            } else {
                LOGGER.log(System.Logger.Level.DEBUG, "Task in-degree decremented, trace ID {0}, in-degree {1}", dependent.getTraceId(), dependent.getInDegree());
            }
        }
        unresolvedNodes.remove(node);
    }
}