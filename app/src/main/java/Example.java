import domains.Command;
import domains.CommandExecutor;
import domains.Scope;
import domains.VolatileAppState;
import domains.root.RootAddValuesCommand;
import domains.root.RootAddValuesRequest;
import domains.root.RootVolatileStatePart;
import domains.root.antenna.AntennaElementStatusRequest;
import domains.root.antenna.AntennaSetValueCommand;
import domains.root.antenna.AntennaSetValueRequest;
import domains.root.antenna.AntennaVolatileStatePart;
import domains.root.error.ErrorSetValueCommand;
import domains.root.error.ErrorSetValueRequest;
import domains.root.error.ErrorVolatileStatePart;
import execution.AtomicReferenceCompareAndSettable;
import execution.BoundedWorkQueue;
import execution.CompareAndSettable;
import execution.Scheduler;
import execution.TaskWorker;
import execution.TraceIdFactory;
import execution.WorkQueue;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Example {

    private static void runWorkers(
            WorkQueue<Command<? extends Scope, ?, ?, ?, ?, ?>> workQueue,
            CompareAndSettable<VolatileAppState> casVolatileAppState) {
        int numberOfWorkers = 10;
        CommandExecutor commandExecutor = new CommandExecutor(
                casVolatileAppState);

        // 1. Create the Virtual Thread executor
        try (ExecutorService vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()) {

            // 2. Submit the long-running workers
            for (int i = 0; i < numberOfWorkers; i++) {
                TaskWorker<Command<? extends Scope, ?, ?, ?, ?, ?>> worker = TaskWorker.create(
                        workQueue, commandExecutor);
                vThreadExecutor.submit(worker);
            }

            System.out.println(
                    "Started " + numberOfWorkers + " virtual thread workers.");

            // 3. Sleep indefinitely until the shutdown hook interrupts this thread
            try {
                Thread.sleep(Long.MAX_VALUE);
            }
            catch (InterruptedException e) {
                System.out.println(
                        "Manager thread received interrupt signal. Initiating worker shutdown...");

                // shutdownNow() sends an interrupt() to all running virtual threads,
                // triggering the graceful shutdown logic inside your TaskWorker class.
                vThreadExecutor.shutdownNow();
            }

            System.out.println(
                    "Awaiting final termination of virtual threads...");

            // 4. The try-with-resources block implicitly calls vThreadExecutor.close() right here.
            // close() acts as a barrier: it blocks the manager thread until all virtual
            // threads have actually finished their current execution and died.
        }

        System.out.println("Manager thread finished cleanly.");
    }

    private static AntennaSetValueCommand getAntennaSetValueCommand(
            TraceIdFactory traceIdFactory, int value) {
        AntennaSetValueRequest.Factory antennaSetValueRequestFactory = AntennaSetValueRequest.getFactory(
                traceIdFactory);

        Optional<AntennaSetValueRequest> maybeAntennaSetValueRequest = antennaSetValueRequestFactory.create(
                value);

        assert maybeAntennaSetValueRequest.isPresent();

        AntennaSetValueRequest antennaSetValueRequest = maybeAntennaSetValueRequest.get();

        return new AntennaSetValueCommand(
                antennaSetValueRequest);
    }

    private static ErrorSetValueCommand getErrorSetValueCommand(
            TraceIdFactory traceIdFactory, int value) {
        ErrorSetValueRequest.Factory errorSetValueRequestFactory = ErrorSetValueRequest.getFactory(
                traceIdFactory);

        Optional<ErrorSetValueRequest> maybeErrorSetValueRequest = errorSetValueRequestFactory.create(
                value);

        assert maybeErrorSetValueRequest.isPresent();

        ErrorSetValueRequest errorSetValueRequest = maybeErrorSetValueRequest.get();

        return new ErrorSetValueCommand(
                errorSetValueRequest);
    }

    private static RootAddValuesCommand getRootAddValuesCommand(
            TraceIdFactory traceIdFactory) {
        RootAddValuesRequest.Factory rootAddValuesRequestFactory = RootAddValuesRequest.getFactory(
                traceIdFactory);

        Optional<RootAddValuesRequest> maybeRootAddValuesRequest = rootAddValuesRequestFactory.create();

        assert maybeRootAddValuesRequest.isPresent();

        RootAddValuesRequest rootAddValuesRequest = maybeRootAddValuesRequest.get();

        return new RootAddValuesCommand(
                rootAddValuesRequest);
    }

    public static void main(String[] ignored) {

        /// Setup

        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.FINE);
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof ConsoleHandler) {
                handler.setLevel(Level.FINE);
            }
        }

        WorkQueue<Command<? extends Scope, ?, ?, ?, ?, ?>> workQueue = BoundedWorkQueue.create(
                1024);

        Scheduler<Command<? extends Scope, ?, ?, ?, ?, ?>> scheduler = Scheduler.create(
                workQueue);

        TraceIdFactory traceIdFactory = new TraceIdFactory() {

            private int nextId = 0;

            @Override
            public String createTraceId() {
                return Integer.toString(nextId++);
            }
        };

        AntennaElementStatusRequest.Factory antennaElementStatusCommandFactory = AntennaElementStatusRequest.getFactory(
                traceIdFactory);

        CompareAndSettable<VolatileAppState> casVolatileAppState = AtomicReferenceCompareAndSettable.of(
                new VolatileAppState(
                        new RootVolatileStatePart(
                                new AntennaVolatileStatePart(false, false, 0),
                                new ErrorVolatileStatePart(0),
                                0
                        )
                ));

        Thread workerThread = new Thread(
                () -> runWorkers(workQueue, casVolatileAppState));
        workerThread.start();

        /// Operation

        scheduler.add(getAntennaSetValueCommand(traceIdFactory, 1));
        scheduler.add(getErrorSetValueCommand(traceIdFactory, 2));
        scheduler.add(getRootAddValuesCommand(traceIdFactory));
        scheduler.add(getAntennaSetValueCommand(traceIdFactory, 3));
        scheduler.add(getErrorSetValueCommand(traceIdFactory, 4));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(
                    "\n[Shutdown Hook] Ctrl-C detected. Interrupting manager thread...");

            // Signal the manager thread to wake up and begin shutdown
            workerThread.interrupt();

            try {
                // 3. Block the shutdown hook until the manager thread finishes gracefully
                workerThread.join();
                System.out.println(
                        "[Shutdown Hook] Graceful shutdown complete. Exiting JVM.");
            }
            catch (InterruptedException e) {
                System.err.println(
                        "[Shutdown Hook] Interrupted while waiting for join.");
                Thread.currentThread().interrupt();
            }
        }, "Shutdown-Hook-Thread"));

    }

}
