package domains.root;

import domains.Scope;
import domains.ScopeNode;

import java.util.List;
import java.util.Objects;

/**
 * The runtime representation of the highest-level execution boundary.
 * <p>
 * {@code RootScope} anchors the execution path. Because there is only one root
 * context per execution tree, all instances of {@code RootScope} are considered
 * equal by definition.
 * </p>
 */
public final class RootScope extends Scope {

    /**
     * A pre-computed, immutable path containing the stateless root node.
     * Caching this prevents unnecessary allocations during frequent path traversals.
     */
    private static final List<ScopeNode> PATH = List.of(new RootScopeNode());

    @Override
    public List<ScopeNode> getPath() {
        return PATH;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof RootScope;
    }

    @Override
    public int hashCode() {
        // Must be overridden when equals() is overridden.
        // Returning a constant hash ensures all RootScopes hash to the same bucket.
        return Objects.hash(RootScope.class);
    }
}