package persistent_data_structures;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * An immutable, persistent singly-linked list (Cons list). This collection does
 * not permit {@code null} elements.
 *
 * @param <T> the type of elements in this list
 */
public final class PersistentList<T> implements Iterable<T>, Serializable {

    // A shared singleton to represent the empty list (Nil)
    @SuppressWarnings("rawtypes")
    private static final PersistentList EMPTY = new PersistentList<>(null, null, 0);
    @Serial
    private static final long serialVersionUID = 1L;
    private final T head;
    private final PersistentList<T> tail;
    private final int size;

    /**
     * Private constructor to enforce the use of static factory methods.
     *
     * @param head the first element of the list (null only for EMPTY)
     * @param tail the rest of the list (null only for EMPTY)
     * @param size the pre-calculated size of the list
     */
    private PersistentList(T head, PersistentList<T> tail, int size) {
        this.head = head;
        this.tail = tail;
        this.size = size;
    }

    /**
     * Returns an empty, immutable PersistentList. Time complexity: O(1)
     *
     * @param <T> the type of elements
     * @return the singleton empty list
     */
    @SuppressWarnings("unchecked")
    public static <T> PersistentList<T> empty() {
        return (PersistentList<T>) EMPTY;
    }

    /**
     * Creates a new list containing a single element. Time complexity: O(1)
     *
     * @param e1  the element to include in the list
     * @param <T> the type of the element
     * @return a new PersistentList containing the provided element
     * @throws NullPointerException if the element is null
     */
    public static <T> PersistentList<T> of(T e1) {
        return PersistentList.<T>empty().prepend(e1);
    }

    /**
     * Creates a new list containing the provided elements. Time complexity:
     * O(1) for a fixed number of elements.
     *
     * @param e1  the first element to include in the list
     * @param e2  the second element to include in the list
     * @param <T> the type of the elements
     * @return a new PersistentList containing the provided elements in the
     * order they were given
     * @throws NullPointerException if any element is null
     */
    public static <T> PersistentList<T> of(T e1, T e2) {
        return PersistentList.<T>empty().prepend(e2).prepend(e1);
    }

    /**
     * Creates a new list containing the provided elements. Time complexity:
     * O(N) where N is the number of elements provided.
     *
     * @param elements the elements to include in the list
     * @param <T>      the type of elements
     * @return a new PersistentList containing the provided elements
     * @throws NullPointerException if any element is null
     */
    @SafeVarargs
    public static <T> PersistentList<T> of(T... elements) {
        PersistentList<T> result = empty();
        // Iterate backwards to maintain the provided order when prepending
        for (int i = elements.length - 1; i >= 0; i--) {
            result = result.prepend(elements[i]);
        }
        return result;
    }

    /**
     * Checks if the list contains no elements. Time complexity: O(1)
     *
     * @return {@code true} if the list is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return this == EMPTY;
    }

    /**
     * Returns the number of elements in this list. Time complexity: O(1)
     *
     * @return the size of the list
     */
    public int size() {
        return size;
    }

    /**
     * Prepends an element to the front of the list. This operation creates a
     * new head node while structurally sharing the existing list as its tail.
     * Time complexity: O(1)
     *
     * @param element the element to add
     * @return a new PersistentList with the element at the front
     * @throws NullPointerException if the element is null
     */
    public PersistentList<T> prepend(T element) {
        Objects.requireNonNull(element, "PersistentList does not permit null elements");
        return new PersistentList<>(element, this, this.size + 1);
    }

    /**
     * Retrieves the first element of the list, if present. Time complexity:
     * O(1)
     *
     * @return an {@link Optional} containing the head element, or
     * {@code Optional.empty()} if the list is empty
     */
    public Optional<T> head() {
        if (isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(head);
    }

    /**
     * Retrieves the list containing all elements except the first, if the list
     * is not empty. Time complexity: O(1)
     *
     * @return an {@link Optional} containing the tail list, or
     * {@code Optional.empty()} if the list is empty
     */
    public PersistentList<T> tail() {
        if (isEmpty()) {
            return PersistentList.empty();
        }
        return tail;
    }

    /**
     * Removes the first occurrence of the specified element from the list. This
     * iterative implementation prevents StackOverflowErrors on deeply nested
     * lists. Time complexity: O(N), Space complexity: O(N)
     *
     * @param item the element to remove
     * @return a new PersistentList without the specified element, or this list
     * if the element was not found
     */
    public PersistentList<T> remove(T item) {
        if (isEmpty()) {
            return this;
        }

        if (Objects.equals(head, item)) {
            return tail;
        }

        // Find the index of the item
        Optional<Integer> maybeIndex = Optional.empty();
        {
            int currentIndex = 0;
            for (final T element : this) {
                if (Objects.equals(element, item)) {
                    maybeIndex = Optional.of(currentIndex);
                    break;
                }
                currentIndex++;
            }
        }

        // Item not found, preserve memory and return original instance
        if (maybeIndex.isEmpty()) {
            return this;
        }

        // Collect prefix nodes in an array exactly sized to the prefix length
        final int index = maybeIndex.get();
        final Object[] prefix = new Object[index];
        PersistentList<T> current = this;
        for (int i = 0; i < index; i++) {
            prefix[i] = current.head;
            current = current.tail;
        }

        // 3. Skip the item to be removed
        PersistentList<T> result = current.tail;

        // 4. Rebuild the list from the shared tail using the prefix buffer
        for (int i = index - 1; i >= 0; i--) {
            @SuppressWarnings("unchecked") final T val = (T) prefix[i];
            result = new PersistentList<>(val, result, result.size + 1);
        }

        return result;
    }

    /**
     * Returns a new list with the elements in reverse order.
     * <p>
     * Time complexity: O(N)
     *
     * @return a reversed PersistentList
     */
    public PersistentList<T> reverse() {
        PersistentList<T> result = PersistentList.empty();
        for (T element : this) {
            result = result.prepend(element);
        }
        return result;
    }

    /**
     * Returns a new list containing only the first {@code n} elements of this
     * list.
     * <p>
     * Time complexity: O(N) where N is the number of elements taken. Space
     * complexity: O(N) where N is the number of elements taken.
     *
     * @param n the number of elements to take from the front of the list
     * @return a new PersistentList containing the first {@code n} elements of
     * this list, or the entire list if {@code n} is greater than or equal to
     * the size of this list
     */
    public PersistentList<T> take(int n) {
        if (n <= 0) {
            return PersistentList.empty();
        }
        if (n >= size) {
            return this;
        }

        PersistentList<T> result = PersistentList.empty();
        PersistentList<T> current = this;
        for (int i = 0; i < n; i++) {
            result = result.prepend(current.head);
            current = current.tail;
        }
        return result.reverse(); // Reverse to maintain original order
    }

    /**
     * Returns a new list with the first {@code n} elements removed.
     * <p>
     * Time complexity: O(N) where N is the number of elements dropped. Space
     * complexity: O(1).
     *
     * @param n the number of elements to remove from the front of the list
     * @return a new PersistentList with the first {@code n} elements removed,
     * or the entire list if {@code n} is less than or equal to zero, or an
     * empty list if {@code n} is greater than or equal to the size of this list
     */
    public PersistentList<T> drop(int n) {
        if (n <= 0) {
            return this;
        }
        if (n >= size) {
            return PersistentList.empty();
        }

        PersistentList<T> current = this;
        for (int i = 0; i < n; i++) {
            current = current.tail;
        }
        return current; // Remaining list after dropping the first n elements
    }

    /**
     * Transforms the elements of this list using the provided mapping function.
     * <p>
     * Time complexity: O(N) where N is the size of this list. Space complexity
     * O(N) where N is the size of this list.
     *
     * @param mapper the function to apply to each element
     * @param <U>    the type of elements in the resulting list
     * @return a new PersistentList containing the results of applying the
     * mapper function to each element of this list
     * @throws NullPointerException if the mapper function is null or if it
     *                              produces a null result for any element
     */
    public <U> PersistentList<U> map(java.util.function.Function<? super T, ? extends U> mapper) {
        PersistentList<U> result = PersistentList.empty();
        for (T element : this) {
            result = result.prepend(mapper.apply(element));
        }
        return result.reverse(); // Reverse to maintain original order
    }

    /**
     * Returns a new list containing only the elements of this list that match
     * the given predicate.
     * <p>
     * Time complexity: O(N) where N is the size of this list. Space complexity
     * O(N) where N is the number of elements that match the predicate.
     *
     * @param predicate the condition to test each element against
     * @return a new PersistentList containing only the elements that satisfy
     * the predicate
     */
    public PersistentList<T> filter(java.util.function.Predicate<? super T> predicate) {
        PersistentList<T> result = PersistentList.empty();
        for (final T element : this) {
            if (predicate.test(element)) {
                result = result.prepend(element);
            }
        }
        return result.reverse(); // Reverse to maintain original order
    }

    /**
     * Reduces the elements of this list into a single value by iteratively
     * applying the provided accumulator function, starting with the given
     * identity value.
     * <p>
     * Time complexity: O(N) where N is the size of this list. Space complexity
     * O(1).
     *
     * @param identity    the initial value for the reduction
     * @param accumulator the function that combines the accumulated value with
     *                    each element of the list
     * @param <U>         the type of the resulting value
     * @return the result of reducing the elements of this list using the
     * accumulator function
     */
    public <U> U foldLeft(U identity, java.util.function.BiFunction<U, ? super T, U> accumulator) {
        U result = identity;
        for (T element : this) {
            result = accumulator.apply(result, element);
        }
        return result;
    }

    /**
     * Appends another list to the end of this list.
     * <p>
     * Time complexity: O(N) where N is the size of THIS list. Space complexity:
     * O(N) where N is the size of THIS list.
     *
     * @param other the list to concatenate to the end of this list
     * @return a new PersistentList containing all elements of this list
     * followed by all elements of the other list
     */
    public PersistentList<T> concat(PersistentList<T> other) {
        if (this.isEmpty()) {
            return other;
        }
        if (other.isEmpty()) {
            return this;
        }

        // We reverse this list, then prepend it onto the other list
        PersistentList<T> result = other;
        for (T element : this.reverse()) {
            result = result.prepend(element);
        }
        return result;
    }

    /**
     * Converts this PersistentList into a standard Java List. Time complexity:
     * O(N) Space complexity: O(N)
     *
     * @return a new List containing the same elements as this PersistentList in
     * the same order
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        for (T element : this) {
            result.add(element);
        }
        return java.util.List.copyOf(result); // Return an unmodifiable copy to preserve immutability
    }

    /**
     * Intercepts the serialization process. Instead of serializing the
     * recursive list, the JVM will serialize the flat proxy object.
     */
    @Serial
    private Object writeReplace() {
        return new SerializationProxy<>(this);
    }

    /**
     * Prevents the default deserialization of this class to protect against
     * stream tampering.
     */
    @Serial
    @ExcludeFromCoverageGeneratedReport
    private void readObject(@SuppressWarnings("unused") java.io.ObjectInputStream stream) throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Serialization proxy required");
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private PersistentList<T> current = PersistentList.this;

            @Override
            public boolean hasNext() {
                return !current.isEmpty();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T value = current.head;
                current = current.tail;
                return value;
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentList)) {
            return false;
        }

        PersistentList<?> that = (PersistentList<?>) o;
        if (this.size != that.size) {
            return false;
        }

        Iterator<T> thisIterator = this.iterator();
        Iterator<?> thatIterator = that.iterator();

        int i = this.size;
        while (i-- > 0) {
            if (!Objects.equals(thisIterator.next(), thatIterator.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        Hasher hasher = new StandardOrderedHasher();
        for (final T element : this) {
            hasher.hash(element);
        }
        return hasher.getHashCode();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        PersistentList<T> current = this;
        while (!current.isEmpty()) {
            sb.append(current.head);
            current = current.tail;
            if (!current.isEmpty()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * A private static proxy class that flattens the list into an array for
     * safe network transit.
     */
    private static class SerializationProxy<T> implements java.io.Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private final Object[] elements;

        SerializationProxy(PersistentList<T> list) {
            this.elements = new Object[list.size()];
            int i = 0;
            for (T element : list) {
                this.elements[i++] = element;
            }
        }

        /**
         * Intercepts the deserialization process. The JVM will replace the
         * proxy with the result of this method.
         */
        @Serial
        @SuppressWarnings("unchecked")
        private Object readResolve() {
            return PersistentList.of((T[]) elements);
        }
    }
}
