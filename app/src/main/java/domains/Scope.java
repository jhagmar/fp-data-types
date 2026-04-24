package domains;

import domains.root.RootScope;
import domains.root.antenna.AntennaScope;
import domains.root.error.ErrorScope;

import java.util.List;
import java.util.Objects;

/**
 * The base representation of an operational boundary within the task execution engine.
 * <p>
 * While {@link ScopeNode} acts as the static definition or blueprint of a boundary,
 * {@code Scope} represents the active, hierarchical instance of that boundary during execution.
 * It is sealed to strictly control the permitted scope types across the system.
 * </p>
 */
public sealed abstract class Scope permits RootScope, AntennaScope, ErrorScope {

    /**
     * Retrieves the hierarchical lineage of this scope, starting from the root node
     * down to the specific node representing this scope.
     * <p>
     * <b>Important:</b> Implementations must ensure this returns an unmodifiable list
     * (e.g., using {@code List.copyOf()}) to prevent accidental corruption of the execution path.
     * </p>
     *
     * @return an ordered, immutable list of {@link ScopeNode}s representing the path.
     */
    protected abstract List<ScopeNode> getPath();

    /**
     * Determines if this scope conflicts with another scope hierarchically.
     * <p>
     * A conflict occurs if one scope's path is a direct ancestor or descendant of the other
     * (i.e., one path is a strict prefix of the other). If the paths diverge at any point
     * (representing parallel or independent execution branches), they do not conflict.
     * </p>
     * <p>
     * This is particularly useful for execution locks, ensuring that a parent scope cannot
     * be mutated or executed concurrently with its own child scope.
     * </p>
     *
     * @param that the other scope to compare against (must not be null)
     * @return {@code true} if the scopes overlap hierarchically, {@code false} if they are independent
     */
    public boolean conflictsWith(Scope that) {
        Objects.requireNonNull(that, "The scope to compare against cannot be null");

        final List<ScopeNode> thisPath = this.getPath();
        final List<ScopeNode> thatPath = that.getPath();

        final int minSize = Math.min(thisPath.size(), thatPath.size());

        // If the shared prefix of both paths is identical, one is an ancestor of the other.
        // List.equals() inherently checks size and ordered element equality.
        return thisPath.subList(0, minSize).equals(thatPath.subList(0, minSize));
    }
}