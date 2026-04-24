package domains.root.antenna;

import domains.ExecutionError;

/**
 * Represents a failure state specifically related to antenna element status updates.
 * <p>
 * This record is used within the {@link fp.Result} monad to communicate why a command
 * could not be successfully processed (e.g., element conflict, hardware lockout).
 * </p>
 */
public record AntennaElementStatusExecutionError() implements ExecutionError<AntennaScope> {
    // Future iteration: add fields like String message or ErrorCode code.
}