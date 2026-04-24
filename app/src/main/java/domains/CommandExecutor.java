package domains;

import domains.root.RootAddValuesCommand;
import domains.root.RootAddValuesExecutionError;
import domains.root.RootAddValuesExecutionOutcome;
import domains.root.RootCommand;
import domains.root.RootContext;
import domains.root.RootPersistentStateReader;
import domains.root.RootVolatileStatePart;
import domains.root.antenna.AntennaCommand;
import domains.root.antenna.AntennaContext;
import domains.root.antenna.AntennaElementStatusCommand;
import domains.root.antenna.AntennaElementStatusExecutionError;
import domains.root.antenna.AntennaElementStatusExecutionOutcome;
import domains.root.antenna.AntennaPersistentStateReader;
import domains.root.antenna.AntennaSetValueCommand;
import domains.root.antenna.AntennaSetValueExecutionError;
import domains.root.antenna.AntennaSetValueExecutionOutcome;
import domains.root.antenna.AntennaVolatileStatePart;
import domains.root.error.ErrorCommand;
import domains.root.error.ErrorContext;
import domains.root.error.ErrorPersistentStateReader;
import domains.root.error.ErrorSetValueCommand;
import domains.root.error.ErrorSetValueExecutionError;
import domains.root.error.ErrorSetValueExecutionOutcome;
import domains.root.error.ErrorVolatileStatePart;
import execution.CompareAndSettable;
import execution.Executor;
import fp.Result;

import java.util.Objects;

public class CommandExecutor implements Executor<Command<
        ? extends Scope, ?, ?, ?, ?, ?
        >> {

    private final static System.Logger LOGGER = System.getLogger(CommandExecutor.class.getName());

    private final CompareAndSettable<VolatileAppState> casVolatileAppState;

    public CommandExecutor(
            CompareAndSettable<VolatileAppState> volatileAppStateGetter) {
        Objects.requireNonNull(volatileAppStateGetter);
        this.casVolatileAppState = volatileAppStateGetter;
    }

    @Override
    public void execute(
            Command<? extends Scope, ?, ?, ?, ?, ?> command)
            throws InterruptedException {

        LOGGER.log(System.Logger.Level.DEBUG, "Executing Command, traceId = {0}", command.getTraceId());

        VolatileAppState newVolatileAppState = switch (command) {
            case AntennaCommand<?, ?> antennaCommand ->
                    executeAntennaCommand(antennaCommand);
            case RootCommand<?, ?> rootCommand ->
                    executeRootCommand(rootCommand);
            case ErrorCommand<?, ?> errorCommand ->
                    executeErrorCommand(errorCommand);
        };

        LOGGER.log(System.Logger.Level.DEBUG, "New state grafted from Command with traceId = {0}:\n{1}", command.getTraceId(), newVolatileAppState);

    }

    private VolatileAppState executeAntennaCommand(AntennaCommand<?, ?> antennaCommand) {
        AntennaVolatileStatePart antennaVolatileStatePart = VolatileAppStateLenses.ANTENNA.get(
                casVolatileAppState.get());

        AntennaVolatileStatePart newAntennaVolatileStatePart = switch (antennaCommand) {
            case AntennaElementStatusCommand antennaElementStatusCommand ->
                    executeAntennaElementStatusCommand(
                            antennaElementStatusCommand,
                            antennaVolatileStatePart);
            case AntennaSetValueCommand antennaSetValueCommand ->
                    executeAntennaSetValueCommand(antennaSetValueCommand,
                                                  antennaVolatileStatePart);
        };

        while (true) {
            VolatileAppState volatileAppState = casVolatileAppState.get();
            VolatileAppState newVolatileAppState = VolatileAppStateLenses.ANTENNA.set(
                    volatileAppState, newAntennaVolatileStatePart);
            if (casVolatileAppState.trySet(volatileAppState,
                                           newVolatileAppState)) {

                return newVolatileAppState;
            }
        }
    }

    private AntennaVolatileStatePart executeAntennaElementStatusCommand(
            AntennaElementStatusCommand antennaElementStatusCommand,
            AntennaVolatileStatePart antennaVolatileStatePart) {
        AntennaPersistentStateReader mockAntennaPersistentStateReader = new AntennaPersistentStateReader() {

        };
        AntennaContext mockAntennaContext = new AntennaContext();
        Result<AntennaElementStatusExecutionOutcome, AntennaElementStatusExecutionError> result
                = antennaElementStatusCommand.apply(mockAntennaContext,
                                                    antennaVolatileStatePart,
                                                    mockAntennaPersistentStateReader);

        assert result.isOk();

        AntennaElementStatusExecutionOutcome outcome = result.unwrap();

        return new AntennaVolatileStatePart(
                outcome.element0Operational(), outcome.element1Operational(),
                antennaVolatileStatePart.antennaValue());
    }

    private AntennaVolatileStatePart executeAntennaSetValueCommand(
            AntennaSetValueCommand antennaSetValueCommand,
            AntennaVolatileStatePart antennaVolatileStatePart) {
        AntennaPersistentStateReader mockAntennaPersistentStateReader = new AntennaPersistentStateReader() {

        };
        AntennaContext mockAntennaContext = new AntennaContext();
        Result<AntennaSetValueExecutionOutcome, AntennaSetValueExecutionError> result
                = antennaSetValueCommand.apply(mockAntennaContext,
                                               antennaVolatileStatePart,
                                               mockAntennaPersistentStateReader);

        assert result.isOk();

        AntennaSetValueExecutionOutcome outcome = result.unwrap();

        return new AntennaVolatileStatePart(
                antennaVolatileStatePart.element0Operational(),
                antennaVolatileStatePart.element1Operational(),
                outcome.value());
    }

    private VolatileAppState executeErrorCommand(ErrorCommand<?, ?> errorCommand) {
        ErrorVolatileStatePart errorVolatileStatePart = VolatileAppStateLenses.ERROR.get(
                casVolatileAppState.get());

        ErrorVolatileStatePart newErrorVolatileStatePart = switch (errorCommand) {
            case ErrorSetValueCommand errorSetValueCommand ->
                    executeErrorSetValueCommand(errorSetValueCommand,
                                                errorVolatileStatePart);
        };

        while (true) {
            VolatileAppState volatileAppState = casVolatileAppState.get();
            VolatileAppState newVolatileAppState = VolatileAppStateLenses.ERROR.set(
                    volatileAppState, newErrorVolatileStatePart);
            if (casVolatileAppState.trySet(volatileAppState,
                                           newVolatileAppState)) {
                return newVolatileAppState;
            }
        }
    }

    private ErrorVolatileStatePart executeErrorSetValueCommand(
            ErrorSetValueCommand errorSetValueCommand,
            ErrorVolatileStatePart errorVolatileStatePart) {
        ErrorPersistentStateReader mockErrorPersistentStateReader = new ErrorPersistentStateReader() {

        };
        ErrorContext mockErrorContext = new ErrorContext();
        Result<ErrorSetValueExecutionOutcome, ErrorSetValueExecutionError> result
                = errorSetValueCommand.apply(mockErrorContext,
                                             errorVolatileStatePart,
                                             mockErrorPersistentStateReader);

        assert result.isOk();

        ErrorSetValueExecutionOutcome outcome = result.unwrap();

        return new ErrorVolatileStatePart(
                outcome.value());
    }

    private VolatileAppState executeRootCommand(RootCommand<?, ?> rootCommand) {
        RootVolatileStatePart rootVolatileStatePart = VolatileAppStateLenses.ROOT.get(
                casVolatileAppState.get());

        RootVolatileStatePart newRootVolatileStatePart = switch (rootCommand) {
            case RootAddValuesCommand rootAddValuesCommand ->
                    executeRootAddValuesCommand(rootAddValuesCommand,
                                                rootVolatileStatePart);
        };

        while (true) {
            VolatileAppState volatileAppState = casVolatileAppState.get();
            VolatileAppState newVolatileAppState = VolatileAppStateLenses.ROOT.set(
                    volatileAppState, newRootVolatileStatePart);
            if (casVolatileAppState.trySet(volatileAppState,
                                           newVolatileAppState)) {
                return newVolatileAppState;
            }
        }
    }

    private RootVolatileStatePart executeRootAddValuesCommand(
            RootAddValuesCommand rootAddValuesCommand,
            RootVolatileStatePart rootVolatileStatePart) {
        RootPersistentStateReader mockRootPersistentStateReader = new RootPersistentStateReader() {

        };
        RootContext mockRootContext = new RootContext();
        Result<RootAddValuesExecutionOutcome, RootAddValuesExecutionError> result
                = rootAddValuesCommand.apply(mockRootContext,
                                             rootVolatileStatePart,
                                             mockRootPersistentStateReader);

        assert result.isOk();

        RootAddValuesExecutionOutcome outcome = result.unwrap();

        return new RootVolatileStatePart(
                rootVolatileStatePart.antenna(), rootVolatileStatePart.error(),
                outcome.value());
    }
}
