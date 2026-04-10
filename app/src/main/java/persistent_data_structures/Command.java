package persistent_data_structures;

interface Command<StatePart, E> {
    Result<StatePart, E> execute(StatePart statePart);
}