package domains;

/**
 * A contract indicating that an entity is strictly bound to a specific operational boundary.
 * <p>
 * Implementing this interface allows components—such as commands, events, or
 * configuration objects—to explicitly declare the domain scope they belong to.
 * This facilitates strict, compile-time routing and boundary enforcement.
 * </p>
 *
 * @param <S> the specific type of {@link Scope} this entity is bound to
 */
public interface Scoped<S extends Scope> {

    /**
     * Retrieves the specific operational boundary or scope associated with this entity.
     *
     * @return the operational scope of type {@code S}
     */
    S getScope();
}