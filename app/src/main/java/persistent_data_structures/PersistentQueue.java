package persistent_data_structures;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * An immutable, persistent queue implemented using two singly-linked lists.
 * This collection does not permit {@code null} elements.
 * <p>
 * This implementation achieves amortized O(1) time complexity for enqueue and
 * dequeue operations by maintaining a front list for dequeuing and a rear list
 * for enqueueing. The rear list is periodically reversed to become the new
 * front list when the front list is exhausted.
 *
 * @param <T> the type of elements in this queue
 */
public final class PersistentQueue<T> implements Iterable<T>, Serializable {

    @SuppressWarnings("rawtypes")
    private static final PersistentQueue EMPTY = new PersistentQueue<>(PersistentList.empty(), PersistentList.empty(), 0);
    @Serial
    private static final long serialVersionUID = 1L;
    private final PersistentList<T> front;
    private final PersistentList<T> rear;
    private final int size;

    /**
     * Private constructor to enforce invariants and rebalance the queue.
     *
     * @param front the list containing the front elements
     * @param rear  the list containing the newly enqueued elements in reverse
     *              order
     * @param size  the total pre-calculated size of the queue
     */
    private PersistentQueue(PersistentList<T> front, PersistentList<T> rear, int size) {
        // Enforce the core invariant: front is empty ONLY if the entire queue is empty.
        if (front.isEmpty() && !rear.isEmpty()) {
            this.front = rear.reverse();
            this.rear = PersistentList.empty();
        } else {
            this.front = front;
            this.rear = rear;
        }
        this.size = size;
    }

    /**
     * Returns an empty, immutable PersistentQueue. Time complexity: O(1)
     *
     * @param <T> the type of elements
     * @return the singleton empty queue
     */
    @SuppressWarnings("unchecked")
    public static <T> PersistentQueue<T> empty() {
        return (PersistentQueue<T>) EMPTY;
    }

    /**
     * Creates a new queue containing a single element. Time complexity: O(1)
     *
     * @param e1  the element to include
     * @param <T> the type of the element
     * @return a new PersistentQueue containing the provided element
     * @throws NullPointerException if the element is null
     */
    public static <T> PersistentQueue<T> of(T e1) {
        return PersistentQueue.<T>empty().enqueue(e1);
    }

    /**
     * Creates a new queue containing the provided elements. Time complexity:
     * O(1) for a fixed number of elements.
     *
     * @param e1  the first element
     * @param e2  the second element
     * @param <T> the type of the elements
     * @return a new PersistentQueue containing the elements in order
     */
    public static <T> PersistentQueue<T> of(T e1, T e2) {
        return PersistentQueue.<T>empty().enqueue(e1).enqueue(e2);
    }

    /**
     * Creates a new queue containing the provided elements. Time complexity:
     * O(N) where N is the number of elements provided.
     *
     * @param elements the elements to include
     * @param <T>      the type of elements
     * @return a new PersistentQueue containing the provided elements
     */
    @SafeVarargs
    public static <T> PersistentQueue<T> of(T... elements) {
        PersistentQueue<T> result = empty();
        for (T element : elements) {
            result = result.enqueue(element);
        }
        return result;
    }

    /**
     * Checks if the queue contains no elements. Time complexity: O(1)
     *
     * @return {@code true} if the queue is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements in this queue. Time complexity: O(1)
     *
     * @return the size of the queue
     */
    public int size() {
        return size;
    }

    /**
     * Adds an element to the back of the queue. Time complexity: O(1)
     *
     * @param element the element to add
     * @return a new PersistentQueue with the element at the back
     * @throws NullPointerException if the element is null
     */
    public PersistentQueue<T> enqueue(T element) {
        Objects.requireNonNull(element, "PersistentQueue does not permit null elements");
        return new PersistentQueue<>(this.front, this.rear.prepend(element), this.size + 1);
    }

    /**
     * Retrieves the first element of the queue, if present. Time complexity:
     * O(1)
     *
     * @return an {@link Optional} containing the head element, or
     * {@code Optional.empty()} if the queue is empty
     */
    public Optional<T> peek() {
        return front.head();
    }

    /**
     * Removes the first element from the queue. Time complexity: Amortized
     * O(1), Worst-case O(N) when a rear-reversal is triggered.
     *
     * @return a new PersistentQueue without the first element, or this queue if
     * it is already empty
     */
    public PersistentQueue<T> dequeue() {
        if (isEmpty()) {
            return this;
        }
        // Tail handles the missing head. Rebalancing is caught by the constructor.
        PersistentList<T> newFront = front.tail();
        return new PersistentQueue<>(newFront, this.rear, this.size - 1);
    }

    /**
     * Transforms the elements of this queue using the provided mapping
     * function. Because the mapper operates element-wise, mapping the internal
     * lists structurally guarantees the exact order is preserved without
     * rebuilding. Time complexity: O(N), Space complexity: O(N)
     *
     * @param mapper the function to apply to each element
     * @param <U>    the type of elements in the resulting queue
     * @return a new PersistentQueue containing the mapped elements
     */
    public <U> PersistentQueue<U> map(java.util.function.Function<? super T, ? extends U> mapper) {
        if (isEmpty()) {
            return empty();
        }
        return new PersistentQueue<>(front.map(mapper), rear.map(mapper), size);
    }

    /**
     * Returns a new queue containing only the elements that match the
     * predicate. Time complexity: O(N), Space complexity: O(N)
     *
     * @param predicate the condition to test each element against
     * @return a new PersistentQueue containing the filtered elements
     */
    public PersistentQueue<T> filter(java.util.function.Predicate<? super T> predicate) {
        if (isEmpty()) {
            return this;
        }
        PersistentList<T> filteredFront = front.filter(predicate);
        PersistentList<T> filteredRear = rear.filter(predicate);
        return new PersistentQueue<>(filteredFront, filteredRear, filteredFront.size() + filteredRear.size());
    }

    /**
     * Reduces the elements of this queue into a single value. Time complexity:
     * O(N), Space complexity: O(1)
     *
     * @param identity    the initial value
     * @param accumulator the function that combines the accumulated value
     * @param <U>         the type of the resulting value
     * @return the reduced result
     */
    public <U> U foldLeft(U identity, java.util.function.BiFunction<U, ? super T, U> accumulator) {
        U result = front.foldLeft(identity, accumulator);
        // We must reverse the rear list to iterate in proper FIFO queue order
        return rear.reverse().foldLeft(result, accumulator);
    }

    /**
     * Converts this PersistentQueue into a standard Java List. Time complexity:
     * O(N), Space complexity: O(N)
     *
     * @return an unmodifiable List containing the same elements in FIFO order
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        for (T element : this) {
            result.add(element);
        }
        return java.util.List.copyOf(result);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private final Iterator<T> frontIterator = front.iterator();
            // Lazy initialization of the rear iterator to avoid unnecessary reversals
            // if the user doesn't iterate through the entire queue.
            private Iterator<T> rearReversedIterator = null;

            @Override
            public boolean hasNext() {
                if (frontIterator.hasNext()) {
                    return true;
                }
                if (rearReversedIterator == null) {
                    rearReversedIterator = rear.reverse().iterator();
                }
                return rearReversedIterator.hasNext();
            }

            @Override
            public T next() {
                if (frontIterator.hasNext()) {
                    return frontIterator.next();
                }
                if (rearReversedIterator == null) {
                    rearReversedIterator = rear.reverse().iterator();
                }
                return rearReversedIterator.next();
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentQueue)) {
            return false;
        }

        PersistentQueue<?> that = (PersistentQueue<?>) o;
        if (this.size != that.size) {
            return false;
        }

        Iterator<T> thisIterator = this.iterator();
        Iterator<?> thatIterator = that.iterator();

        while (thisIterator.hasNext()) {
            if (!Objects.equals(thisIterator.next(), thatIterator.next())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        Hasher hasher = new StandardOrderedHasher();
        for (T element : this) {
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
        Iterator<T> it = iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Intercepts the serialization process to use a flat proxy object.
     */
    @Serial
    private Object writeReplace() {
        return new SerializationProxy<>(this);
    }

    /**
     * Prevents default deserialization.
     */
    @Serial
    @ExcludeFromCoverageGeneratedReport
    private void readObject(@SuppressWarnings("unused") java.io.ObjectInputStream stream) throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Serialization proxy required");
    }

    /**
     * Serialization proxy to safely flatten and transmit the queue.
     */
    private static class SerializationProxy<T> implements java.io.Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private final Object[] elements;

        SerializationProxy(PersistentQueue<T> queue) {
            this.elements = new Object[queue.size()];
            int i = 0;
            for (T element : queue) {
                this.elements[i++] = element;
            }
        }

        @Serial
        @SuppressWarnings("unchecked")
        private Object readResolve() {
            return PersistentQueue.of((T[]) elements);
        }
    }
}
