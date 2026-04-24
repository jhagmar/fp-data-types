package execution;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Scheduler Specification (Pure JUnit 5)")
public class SchedulerTest {

    private StubWorkQueue<TestPayload> workQueue;
    private Scheduler<TestPayload> scheduler;

    @BeforeEach
    public void setUp() {
        workQueue = new StubWorkQueue<>();
        scheduler = Scheduler.create(workQueue);
    }

    /**
     * A payload that conflicts if any of its assigned 'resources' overlap.
     */
    public static class TestPayload implements ConflictAware<TestPayload>, Traceable {
        private final String id;
        private final Set<String> resources;

        public TestPayload(String id, String... resources) {
            this.id = id;
            this.resources = Set.of(resources);
        }

        @Override
        public boolean conflictsWith(TestPayload other) {
            return resources.stream().anyMatch(other.resources::contains);
        }

        @Override public String getTraceId() { return id; }
    }

    public static class StubWorkQueue<P extends Traceable> implements WorkQueue<P> {
        public final List<Task<P>> enqueuedTasks = new ArrayList<>();
        public boolean acceptTasks = true;

        @Override
        public boolean tryEnqueue(Task<P> task) {
            if (acceptTasks) {
                enqueuedTasks.add(task);
                return true;
            }
            return false;
        }

        @Override public Task<P> take() { return null; }
    }

    @Nested
    @DisplayName("Task Submission (add)")
    public class TaskSubmission {

        @Test
        @DisplayName("Should enqueue task immediately if no conflicts exist")
        public void shouldEnqueueWhenFree() {
            TestPayload payload = new TestPayload("task-1", "res-A");
            
            scheduler.add(payload);

            assertEquals(1, workQueue.enqueuedTasks.size());
            assertEquals("task-1", workQueue.enqueuedTasks.get(0).getTraceId());
        }

        @Test
        @DisplayName("Should block task and create dependency if conflict is found")
        public void shouldBlockOnConflict() {
            TestPayload p1 = new TestPayload("task-1", "res-A");
            TestPayload p2 = new TestPayload("task-2", "res-A"); // Conflicts on res-A

            scheduler.add(p1);
            scheduler.add(p2);

            assertEquals(1, workQueue.enqueuedTasks.size(), "Only the first task should be enqueued");
            assertEquals("task-1", workQueue.enqueuedTasks.get(0).getTraceId());
        }

        @Test
        @DisplayName("Should fail-fast if payload is null")
        public void shouldThrowOnNull() {
            assertThrows(NullPointerException.class, () -> scheduler.add(null));
        }

        @Test
        @DisplayName("Should handle backpressure if the queue is full")
        public void shouldHandleBackpressureGracefully() {
            workQueue.acceptTasks = false;
            TestPayload p = new TestPayload("task-1", "res-A");

            // Exercises the logger/error branch in tryEnqueue
            assertDoesNotThrow(() -> scheduler.add(p));
            assertTrue(workQueue.enqueuedTasks.isEmpty());
        }
    }

    @Nested
    @DisplayName("Task Resolution (onTaskResolved)")
    public class TaskResolution {

        @Test
        @DisplayName("Should dispatch dependent tasks once their blockers are resolved")
        public void shouldDispatchDependentsOnResolution() {
            scheduler.add(new TestPayload("blocker", "res-A"));
            scheduler.add(new TestPayload("dependent", "res-A"));

            // Task 0 is the blocker. Resolving it should trigger dependent.
            workQueue.enqueuedTasks.get(0).resolve();

            assertEquals(2, workQueue.enqueuedTasks.size());
            assertEquals("dependent", workQueue.enqueuedTasks.get(1).getTraceId());
        }

        @Test
        @DisplayName("Should only dispatch when ALL dependencies are resolved")
        public void shouldWaitForMultipleBlockers() {
            scheduler.add(new TestPayload("b1", "res-A"));
            scheduler.add(new TestPayload("b2", "res-B"));
            scheduler.add(new TestPayload("dep", "res-A", "res-B")); // Blocked by both

            assertEquals(2, workQueue.enqueuedTasks.size());

            // Resolve first blocker
            workQueue.enqueuedTasks.get(0).resolve();
            assertEquals(2, workQueue.enqueuedTasks.size(), "Dependent should still be blocked by b2");

            // Resolve second blocker
            workQueue.enqueuedTasks.get(1).resolve();
            assertEquals(3, workQueue.enqueuedTasks.size(), "Dependent should now be enqueued");
        }

        @Test
        @DisplayName("Should remove resolved tasks from internal tracking")
        public void shouldClearResolvedTasks() {
            TestPayload p1 = new TestPayload("p1", "res-A");
            TestPayload p2 = new TestPayload("p2", "res-A");

            scheduler.add(p1);
            workQueue.enqueuedTasks.get(0).resolve();

            // Adding p2 now should not conflict because p1 is resolved and removed
            scheduler.add(p2);

            assertEquals(2, workQueue.enqueuedTasks.size());
            assertEquals("p2", workQueue.enqueuedTasks.get(1).getTraceId());
        }
    }

    @Nested
    @DisplayName("Factory and Infrastructure")
    public class Factory {

        @Test
        @DisplayName("Should throw NPE on null readyQueue")
        public void shouldValidateQueue() {
            assertThrows(NullPointerException.class, () -> Scheduler.create(null));
        }
    }
}