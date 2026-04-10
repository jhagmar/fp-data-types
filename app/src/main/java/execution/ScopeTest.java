package execution;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

sealed interface ScopeNode
        permits RootScopeNode, AntennaScopeNode, ErrorScopeNode {

}

record RootScopeNode() implements ScopeNode {

}

record AntennaScopeNode() implements ScopeNode {

}

record ErrorScopeNode() implements ScopeNode {

}

sealed abstract class Scope permits RootScope, AntennaScope, ErrorScope {

    abstract List<ScopeNode> getPath();

    public boolean conflictsWith(Scope that) {
        final List<ScopeNode> thisPath = this.getPath();
        final List<ScopeNode> thatPath = that.getPath();

        final int minSize = Math.min(thisPath.size(), thatPath.size());
        for (int i = 0; i < minSize; i++) {
            if (!thisPath.get(i).equals(thatPath.get(i))) {
                return false;
            }
        }
        return true;
    }
}

final class RootScope extends Scope {

    @Override
    public boolean equals(Object other) {
        return other instanceof RootScope;
    }

    @Override
    public List<ScopeNode> getPath() {
        return List.of(new RootScopeNode());
    }
}

final class AntennaScope extends Scope {

    @Override
    public boolean equals(Object other) {
        return other instanceof AntennaScope;
    }

    @Override
    public List<ScopeNode> getPath() {
        return List.of(new RootScopeNode(), new AntennaScopeNode());
    }
}

final class ErrorScope extends Scope {

    @Override
    public boolean equals(Object other) {
        return other instanceof ErrorScope;
    }

    @Override
    public List<ScopeNode> getPath() {
        return List.of(new RootScopeNode(), new ErrorScopeNode());
    }
}

sealed interface Request<S extends Scope> permits AntennaRequest {

}

sealed interface AntennaRequest extends Request<AntennaScope>
        permits AntennaElementStatusRequest {

}

final class AntennaElementStatusRequest implements AntennaRequest {

    final int element;
    final boolean operational;

    private AntennaElementStatusRequest(int element, boolean operational) {
        this.element = element;
        this.operational = operational;
    }

    public int getElement() {
        return element;
    }

    public boolean isOperational() {
        return operational;
    }

    public static Optional<AntennaElementStatusRequest> of(int element,
                                                           boolean operational) {
        if (element < 0 || element > 1) {
            return Optional.empty();
        }
        return Optional.of(
                new AntennaElementStatusRequest(element, operational));
    }
}

sealed interface VolatileStatePart<S extends Scope>
        permits RootVolatileStatePart, AntennaVolatileStatePart,
        ErrorVolatileStatePart {

}

record RootVolatileStatePart(AntennaVolatileStatePart antenna,
                             ErrorVolatileStatePart error)
        implements VolatileStatePart<RootScope> {

}

record AntennaVolatileStatePart(boolean element0Operational,
                                boolean element1Operational)
        implements VolatileStatePart<AntennaScope> {

}

record ErrorVolatileStatePart() implements VolatileStatePart<ErrorScope> {

}

record VolatileAppState(RootVolatileStatePart root) {

}

sealed interface Lens<S extends Scope, SP extends VolatileStatePart<S>>
        permits AntennaLens {

    SP get(VolatileAppState state);

    VolatileAppState set(VolatileAppState original, SP statePart);
}

final class AntennaLens
        implements Lens<AntennaScope, AntennaVolatileStatePart> {

    @Override
    public AntennaVolatileStatePart get(VolatileAppState state) {
        return state.root().antenna();
    }

    @Override
    public VolatileAppState set(VolatileAppState original,
                                AntennaVolatileStatePart antenna) {
        return new VolatileAppState(
                new RootVolatileStatePart(antenna, original.root().error()));
    }
}

sealed interface PersistentStateReader<S extends Scope>
        permits RootPersistentStateReader, AntennaPersistentStateReader,
        ErrorPersistentStateReader {

}

non-sealed interface RootPersistentStateReader
        extends PersistentStateReader<RootScope> {

}

non-sealed interface AntennaPersistentStateReader
        extends PersistentStateReader<AntennaScope> {

}

non-sealed interface ErrorPersistentStateReader
        extends PersistentStateReader<ErrorScope> {

}

sealed interface ExecutionOutcome<S extends Scope>
        permits AntennaExecutionOutcome {

}

sealed interface AntennaExecutionOutcome extends ExecutionOutcome<AntennaScope>
        permits AntennaElementStatusExecutionOutcome {

}

record AntennaElementStatusExecutionOutcome(boolean element0Operational,
                                            boolean element1Operational)
        implements AntennaExecutionOutcome {

}

sealed interface Command<S extends Scope, VSP extends VolatileStatePart<S>, PSR extends PersistentStateReader<S>>
        permits AntennaCommand {

    ExecutionOutcome<S> apply(VSP volatileStatePart, PSR persistentStateReader);
}

sealed interface AntennaCommand extends
        Command<AntennaScope, AntennaVolatileStatePart, AntennaPersistentStateReader>
        permits AntennaElementStatusCommand {

}

final class AntennaElementStatusCommand implements AntennaCommand {

    private final AntennaElementStatusRequest request;

    public AntennaElementStatusCommand(AntennaElementStatusRequest request) {
        this.request = request;
    }

    @Override
    public ExecutionOutcome<AntennaScope> apply(
            AntennaVolatileStatePart volatileStatePart,
            AntennaPersistentStateReader persistentStateReader) {
        System.out.println("     [Command] Executing pure business logic...");
        final boolean element0Operational = request.getElement() == 0
                ? request.isOperational()
                : volatileStatePart.element0Operational();
        final boolean element1Operational = request.getElement() == 1
                ? request.isOperational()
                : volatileStatePart.element1Operational();
        return new AntennaElementStatusExecutionOutcome(element0Operational,
                element1Operational);
    }
}

public class ScopeTest {

    public static void main(String[] args) {
        // 1. Initialize Global App State
        VolatileAppState initialState = new VolatileAppState(
                new RootVolatileStatePart(
                        new AntennaVolatileStatePart(false, false),
                        new ErrorVolatileStatePart()
                )
        );
        AtomicReference<VolatileAppState> casGuard = new AtomicReference<>(
                initialState);
        System.out.println("1. Initial App State: " + casGuard.get());

        // 2. The Edge Adapter (Translating external input to opaque request)
        // This represents a generic message entering the system
        Request<?> opaqueRequest = AntennaElementStatusRequest.of(0, true)
                .orElseThrow();
        System.out.println(
                "2. Ingested Opaque Request: " + opaqueRequest.getClass()
                        .getSimpleName());

        // 3. The Worker Thread (Pattern Matching on the Opaque Request)
        System.out.println("3. Worker thread picking up request...");

        switch (opaqueRequest) {
            case AntennaRequest antennaRequest -> {
                // A. Dependency Resolution
                AntennaLens lens = new AntennaLens();
                AntennaPersistentStateReader dbReader = new AntennaPersistentStateReader() {

                }; // Mock DB Reader

                // B. Translate Request to Command
                AntennaCommand command = switch (antennaRequest) {
                    case AntennaElementStatusRequest antennaElementStatusRequest -> new AntennaElementStatusCommand(
                            antennaElementStatusRequest);
                };

                // C. The Execution & Reconciliation Loop (CAS Loop)
                boolean grafted = false;
                while (!grafted) {
                    // i. Read Global State
                    VolatileAppState globalState = casGuard.get();

                    // ii. Extract state part via Lens
                    AntennaVolatileStatePart volatileStatePart = lens.get(
                            globalState);

                    // iii. Pure Apply
                    ExecutionOutcome<AntennaScope> outcome = command.apply(
                            volatileStatePart, dbReader);

                    // iv. Memory Reconciliation (Project outcome back to a RAM part)
                    if (outcome instanceof AntennaElementStatusExecutionOutcome parsedOutcome) {
                        System.out.println(
                                "     [Worker] Received outcome: " + parsedOutcome);

                        AntennaVolatileStatePart newVolatileStatePart = new AntennaVolatileStatePart(
                                parsedOutcome.element0Operational(),
                                parsedOutcome.element1Operational()
                        );

                        // v. Graft the new part into a new Global State tree via Lens
                        VolatileAppState newGlobalState = lens.set(globalState,
                                newVolatileStatePart);

                        // vi. CAS Commit
                        grafted = casGuard.compareAndSet(globalState,
                                newGlobalState);
                        if (grafted) {
                            System.out.println(
                                    "     [Worker] Successfully CAS grafted state.");
                        } else {
                            System.out.println(
                                    "     [Worker] CAS failed, retrying...");
                        }
                    }
                }

            }
        }

        System.out.println("4. Final App State: " + casGuard.get());
    }
}
