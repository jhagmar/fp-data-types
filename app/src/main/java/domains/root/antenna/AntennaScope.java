package domains.root.antenna;

import domains.Scope;
import domains.ScopeNode;
import domains.root.RootScopeNode;

import java.util.List;
import java.util.Objects;

/**
 * The runtime representation of the antenna execution boundary.-
 */
public final class AntennaScope extends Scope {

    /**
     * A pre-computed, immutable path.
     */
    private static final List<ScopeNode> PATH = List.of(new RootScopeNode(), new AntennaScopeNode());

    @Override
    public List<ScopeNode> getPath() {
        return PATH;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof AntennaScope;
    }

    @Override
    public int hashCode() {
        return Objects.hash(AntennaScope.class);
    }

}