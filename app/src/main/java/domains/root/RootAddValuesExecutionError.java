package domains.root;

import domains.ExecutionError;

public record RootAddValuesExecutionError() implements
        ExecutionError<RootScope> {
}