package execution;

import fp_types.Result;

interface Command<StatePart, E> {
    Result<StatePart, E> execute(StatePart statePart);
}