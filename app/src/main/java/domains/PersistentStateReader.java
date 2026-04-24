package domains;

import domains.root.RootPersistentStateReader;
import domains.root.antenna.AntennaPersistentStateReader;
import domains.root.error.ErrorPersistentStateReader;

/**
 * Represents a service or repository port responsible for retrieving the persistent
 * state of a specific execution scope.
 * <p>
 * In this engine's architecture, state is strictly divided between volatile (in-memory)
 * and persistent (database/event store) layers. This interface defines the read-only
 * operations for fetching that stored data, ensuring that I/O operations are abstracted
 * away from the core execution logic.
 * </p>
 * <p>
 * The generic parameter {@code <S extends Scope>} ensures strict compile-time safety,
 * guaranteeing that a reader can only fetch data relevant to its specific domain boundary.
 * </p>
 *
 * @param <S> the specific {@link Scope} boundary this reader is responsible for
 */
public sealed interface PersistentStateReader<S extends Scope>
        permits RootPersistentStateReader, AntennaPersistentStateReader,
        ErrorPersistentStateReader {
}