package domains.root.error;

import domains.Scope;
import domains.ScopeNode;
import domains.root.RootScopeNode;

import java.util.List;
import java.util.Objects;

/**
 * The runtime representation of the system's error domain.
 */
public final class ErrorScope extends Scope {

    /**
     * A pre-computed, immutable path representing the static hierarchy of the error domain.
     * Caching this prevents unnecessary memory allocation during frequent path traversals.
     */
    private static final List<ScopeNode> PATH = List.of(new RootScopeNode(), new ErrorScopeNode());

    @Override
    public List<ScopeNode> getPath() {
        return PATH;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ErrorScope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ErrorScope.class);
    }
}