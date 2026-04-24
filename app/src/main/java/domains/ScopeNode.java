package domains;

import domains.root.RootScopeNode;
import domains.root.antenna.AntennaScopeNode;
import domains.root.error.ErrorScopeNode;

/**
 * The foundational marker interface representing a distinct operational context
 * or boundary within the domain-driven task execution engine.
 * <p>
 * This interface is strictly sealed to guarantee exhaustive compile-time evaluation
 * of scope types. By limiting implementations to known domain boundaries, the
 * execution engine can safely route, execute, or handle state transitions using
 * pattern matching without the risk of encountering unknown scope variants.
 * </p>
 *
 * @see RootScopeNode
 * @see AntennaScopeNode
 * @see ErrorScopeNode
 */
public sealed interface ScopeNode
        permits RootScopeNode, AntennaScopeNode, ErrorScopeNode {

}