package execution;

/**
 * Represents an entity that can be resolved or executed by the engine.
 * <p>
 * Implementing this interface indicates that the object encapsulates
 * logic or state transitions that need to be triggered during the
 * execution lifecycle.
 * </p>
 */
public interface Resolvable {

    /**
     * Executes the core resolution logic.
     * <p>
     * Depending on the engine's design, this could involve processing
     * data, transitioning task states, or dispatching downstream events.
     * </p>
     */
    void resolve();
}