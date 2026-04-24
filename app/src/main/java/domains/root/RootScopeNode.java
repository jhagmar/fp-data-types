package domains.root;

import domains.ScopeNode;

/**
 * Represents the highest-level boundary in the domain-driven task execution engine.
 * <p>
 * The {@code RootScopeNode} serves as the global entry point or absolute parent
 * for a specific execution tree. All subsequent scopes (like antennas or error)
 * branch out from or operate under the umbrella of this root scope.
 * </p>
 * <p>
 * As an immutable record, this node safely guarantees its identity throughout
 * the execution lifecycle, making it ideal for use in routing tables or state registries.
 * </p>
 */
public record RootScopeNode() implements ScopeNode {
    
}