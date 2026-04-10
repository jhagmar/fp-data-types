package persistent_data_structures;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * A Lens is a functional reference to a substructure within a larger immutable
 * data structure. It provides a way to read, write, and modify a deeply nested
 * property while returning a newly constructed immutable copy of the whole
 * structure.
 *
 * <p>
 * A well-behaved Lens should satisfy the following optical laws:
 * <ul>
 * <li><b>Get-Put:</b> If you get a part and immediately set it back, the whole
 * remains unchanged. ({@code set(whole, get(whole)) == whole})</li>
 * <li><b>Put-Get:</b> If you set a part and then get it, you get exactly what
 * you just set. ({@code get(set(whole, part)) == part})</li>
 * </ul>
 *
 * @param <Whole> The type of the complex, larger immutable object.
 * @param <Part>  The type of the focused sub-property.
 */
public final class Lens<Whole, Part> {

    private final Function<Whole, Part> getter;
    private final BiFunction<Whole, Part, Whole> setter;

    /**
     * Constructs a new Lens. Consider using the static {@link #of} factory
     * method for a fluent API.
     *
     * @param getter A function that extracts the Part from the Whole. Must not
     *               be null.
     * @param setter A function that takes the Whole and a new Part, returning a
     *               new Whole. Must not be null.
     */
    private Lens(Function<Whole, Part> getter, BiFunction<Whole, Part, Whole> setter) {
        this.getter = Objects.requireNonNull(getter, "Getter function must not be null");
        this.setter = Objects.requireNonNull(setter, "Setter function must not be null");
    }

    /**
     * Static factory method for creating a Lens.
     *
     * @param getter A function that extracts the Part from the Whole.
     * @param setter A function that takes the Whole and a new Part, returning a
     *               new Whole.
     * @param <W>    The type of the Whole.
     * @param <P>    The type of the Part.
     * @return A new Lens instance.
     */
    public static <W, P> Lens<W, P> of(Function<W, P> getter, BiFunction<W, P, W> setter) {
        return new Lens<>(getter, setter);
    }

    /**
     * Extracts the focused part from the whole structure.
     *
     * @param whole The parent structure to read from.
     * @return The extracted sub-property.
     */
    public Part get(Whole whole) {
        return getter.apply(whole);
    }

    /**
     * Creates a new copy of the whole structure with the focused part replaced
     * by the new value.
     *
     * @param whole   The original parent structure.
     * @param newPart The new value to set at the focused location.
     * @return A new immutable instance of the Whole with the updated Part.
     */
    public Whole set(Whole whole, Part newPart) {
        return setter.apply(whole, newPart);
    }

    /**
     * Modifies the focused part by applying a function to its current value,
     * returning a new copy of the whole structure.
     *
     * @param whole    The original parent structure.
     * @param modifier A function to apply to the current Part. Must not be
     *                 null.
     * @return A new immutable instance of the Whole with the modified Part.
     */
    public Whole modify(Whole whole, UnaryOperator<Part> modifier) {
        Objects.requireNonNull(modifier, "Modifier function must not be null");
        Part currentPart = get(whole);
        Part updatedPart = modifier.apply(currentPart);
        return set(whole, updatedPart);
    }

    /**
     * Composes this Lens with another Lens, creating a new Lens that focuses on
     * a deeper nested sub-property.
     *
     * @param other     The Lens to append to this path. Must not be null.
     * @param <SubPart> The type of the deeper nested property.
     * @return A new composed Lens targeting the SubPart.
     */
    public <SubPart> Lens<Whole, SubPart> compose(Lens<Part, SubPart> other) {
        Objects.requireNonNull(other, "Lens to compose with must not be null");
        return new Lens<>(
                whole -> other.get(this.get(whole)),
                (whole, newSubPart) -> {
                    Part currentPart = this.get(whole);
                    Part updatedPart = other.set(currentPart, newSubPart);
                    return this.set(whole, updatedPart);
                }
        );
    }
}
