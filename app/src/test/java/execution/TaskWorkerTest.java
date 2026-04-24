package execution;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaskWorker Full Branch Coverage Specification")
public class TaskWorkerTest {

    private StubWorkQueue workQueue;
    private TaskWorker<StubPayload> worker;

    @BeforeEach
    public void setUp() {
        workQueue = new StubWorkQueue();
    }

    public static class StubPayload implements Traceable {
        @Override public String getTraceId() { return "trace-123"; }
    }

    public static class StubTask implements Task<StubPayload> {
        public final AtomicBoolean resolved = new AtomicBoolean(false);
        @Override public StubPayload getPayload() { return new StubPayload(); }
        @Override public String getTraceId() { return "trace-123"; }
        @Override public void resolve() { resolved.set(true); }
    }

    public static class StubWorkQueue implements WorkQueue<StubPayload> {
        private final BlockingQueue<Task<StubPayload>> internal = new ArrayBlockingQueue<>(1);
        private final AtomicInteger takeCount = new AtomicInteger(0);

        @Override public boolean tryEnqueue(Task<StubPayload> payload) { return internal.offer(payload); }
        
        @Override 
        public Task<StubPayload> take() throws InterruptedException { 
            takeCount.incrementAndGet();
            return internal.take(); 
        }

        public int getTakeCount() { return takeCount.get(); }
    }

    public static class InterrupterExecutor implements Executor<StubPayload> {
        @Override
        public void execute(StubPayload payload) throws InterruptedException {
            throw new InterruptedException("Simulated mid-execution interrupt");
        }
    }

    @Test
    @DisplayName("Branch: while condition is FALSE (Loop Skip)")
    public void shouldExitImmediatelyIfInterruptFlagSetBeforeStart() {
        // We set the interrupt flag on the current thread before running
        // This ensures the 'while' condition is evaluated as FALSE immediately.
        Thread.currentThread().interrupt();
        
        try {
            // Execution should enter run(), check while condition, see it's false, and exit.
            worker = TaskWorker.create(workQueue, p -> {});
            worker.run();
        } finally {
            // ALWAYS clear the interrupt flag so we don't affect subsequent tests
            Thread.interrupted(); 
        }

        assertEquals(0, workQueue.getTakeCount(), 
            "Worker should have exited before ever calling workQueue.take()");
    }

    @Test
    @DisplayName("Branch: while condition is TRUE (Loop Entry)")
    public void shouldEnterLoopAndProcessTask() throws InterruptedException {
        // Standard success path to cover the 'true' branch of the while condition.
        AtomicBoolean executed = new AtomicBoolean(false);
        worker = TaskWorker.create(workQueue, p -> executed.set(true));
        
        StubTask task = new StubTask();
        workQueue.tryEnqueue(task);

        Thread t = new Thread(worker);
        t.start();

        assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
            while (!executed.get()) { Thread.sleep(10); }
        });

        t.interrupt();
        t.join();
        
        assertTrue(executed.get());
        assertTrue(task.resolved.get());
    }

    @Test
    @DisplayName("Branch 3a: Catch and rethrow InterruptedException during execution")
    public void shouldReachBranch3aAndResolve() throws InterruptedException {
        InterrupterExecutor interruptingExecutor = new InterrupterExecutor();
        worker = TaskWorker.create(workQueue, interruptingExecutor);
        StubTask task = new StubTask();
        workQueue.tryEnqueue(task);

        Thread workerThread = new Thread(worker);

        assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            workerThread.start();
            workerThread.join(); 
            
            assertTrue(task.resolved.get(), "Task must be resolved even when rethrown");
            assertFalse(workerThread.isAlive(), "Worker thread should have exited");
        });
    }

    @Test
    @DisplayName("Branch 3b: Catch unhandled RuntimeException and continue")
    public void shouldReachBranch3bAndContinue() throws InterruptedException {
        AtomicBoolean secondTaskExecuted = new AtomicBoolean(false);
        Executor<StubPayload> failureExecutor = p -> {
            if (!secondTaskExecuted.get()) {
                throw new RuntimeException("Branch 3b Trigger");
            }
        };

        worker = TaskWorker.create(workQueue, failureExecutor);
        StubTask task1 = new StubTask();
        StubTask task2 = new StubTask();
        
        workQueue.tryEnqueue(task1);

        Thread workerThread = new Thread(worker);
        workerThread.start();

        assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
            while (!task1.resolved.get()) { Thread.sleep(5); }
        });

        secondTaskExecuted.set(true);
        workQueue.tryEnqueue(task2);

        assertTimeoutPreemptively(Duration.ofMillis(500), () -> {
            while (!task2.resolved.get()) { Thread.sleep(5); }
        });

        workerThread.interrupt();
        workerThread.join();

        assertTrue(task1.resolved.get());
        assertTrue(task2.resolved.get());
    }

    @Test
    @DisplayName("Outer Catch: Interruption while blocked on take()")
    public void shouldHandleInterruptionDuringQueueTake() throws InterruptedException {
        worker = TaskWorker.create(workQueue, p -> {});
        Thread workerThread = new Thread(worker);
        
        workerThread.start();
        // Give it a moment to block on workQueue.take()
        Thread.sleep(50);
        
        workerThread.interrupt();
        workerThread.join(500);
        
        assertFalse(workerThread.isAlive(), "Worker should have exited via the outer catch block");
    }
}