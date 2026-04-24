package execution;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BoundedWorkQueue Specification")
public class BoundedWorkQueueTest {

    private static final int CAPACITY = 2;
    private BoundedWorkQueue<TestPayload> workQueue;

    @BeforeEach
    public void setUp() {
        workQueue = BoundedWorkQueue.create(CAPACITY);
    }

    private static class TestPayload implements ConflictAware<TestPayload>, Traceable {
        @Override public String getTraceId() { return "test-id"; }
        @Override public boolean conflictsWith(TestPayload other) { return false; }
    }

    private static class TestTask implements Task<TestPayload> {
        private final TestPayload payload = new TestPayload();
        @Override public TestPayload getPayload() { return payload; }
        @Override public void resolve() {}
        @Override public String getTraceId() { return payload.getTraceId(); }
    }

    @Nested
    @DisplayName("Creation and Validation")
    public class Creation {

        @Test
        @DisplayName("Should create queue with valid capacity")
        public void shouldCreateWithValidCapacity() {
            assertNotNull(BoundedWorkQueue.create(1));
            assertNotNull(BoundedWorkQueue.create(100));
        }

        @Test
        @DisplayName("Should throw exception if capacity is less than 1")
        public void shouldThrowOnInvalidCapacity() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, 
                () -> BoundedWorkQueue.create(0));
            assertEquals("Capacity must be at least 1", ex.getMessage());
        }
    }

    @Nested
    @DisplayName("Enqueue Operations")
    public class Enqueue {

        @Test
        @DisplayName("tryEnqueue should return true when space is available")
        public void shouldEnqueueSuccessfully() {
            assertTrue(workQueue.tryEnqueue(new TestTask()));
        }

        @Test
        @DisplayName("tryEnqueue should return false when queue is full")
        public void shouldReturnFalseWhenFull() {
            workQueue.tryEnqueue(new TestTask());
            workQueue.tryEnqueue(new TestTask());

            boolean result = workQueue.tryEnqueue(new TestTask());
            assertFalse(result, "Should not be able to enqueue past capacity");
        }

        @Test
        @DisplayName("tryEnqueue should throw NullPointerException for null input")
        public void shouldThrowOnNull() {
            assertThrows(NullPointerException.class, () -> workQueue.tryEnqueue(null));
        }
    }

    @Nested
    @DisplayName("Blocking and Dequeue Operations")
    public class Dequeue {

        @Test
        @DisplayName("take() should retrieve items in FIFO order")
        public void shouldRetrieveInOrder() throws InterruptedException {
            TestTask task1 = new TestTask();
            TestTask task2 = new TestTask();

            workQueue.tryEnqueue(task1);
            workQueue.tryEnqueue(task2);

            assertEquals(task1, workQueue.take());
            assertEquals(task2, workQueue.take());
        }

        @Test
        @DisplayName("take() should block until an item is available")
        public void shouldBlockUntilAvailable() {
            AtomicReference<Task<TestPayload>> result = new AtomicReference<>();
            
            // Start a thread that will block on take()
            Thread thread = new Thread(() -> {
                try {
                    result.set(workQueue.take());
                } catch (InterruptedException ignored) {}
            });

            assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
                thread.start();
                
                // Ensure thread is likely waiting
                Thread.sleep(100); 
                
                TestTask task = new TestTask();
                workQueue.tryEnqueue(task);
                
                thread.join();
                assertEquals(task, result.get());
            });
        }

        @Test
        @DisplayName("take() should respect thread interruption")
        public void shouldHandleInterruption() {
            Thread thread = new Thread(() -> {
                assertThrows(InterruptedException.class, () -> workQueue.take());
            });

            assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
                thread.start();
                thread.interrupt(); // Signal the block to break
                thread.join();
            });
        }
    }
}