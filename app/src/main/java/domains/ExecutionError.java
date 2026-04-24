package domains;

/**
 * The foundational contract for domain-specific errors encountered during command execution.
 * <p>
 * By utilizing the generic parameter {@code <S extends Scope>}, errors are strictly
 * categorized by their operational boundary.
 * </p>
 *
 * @param <S> the specific {@link Scope} boundary where this error originated
 */
public interface ExecutionError<S extends Scope> {
    // Implementations are ideally immutable records.
}