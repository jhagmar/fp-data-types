package persistent_data_structures;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * An immutable, persistent priority queue based on a Min-Leftist Heap.
 * Insertions, minimum deletions, and merges are guaranteed O(log N) worst-case time.
 * Looking up the minimum element is O(1).
 * This collection does not permit {@code null} elements. Elements are ordered
 * according to their natural ordering.
 *
 * @param <E> the type of elements held in this queue (must be Comparable)
 */
public final class PersistentPriorityQueue<E extends Comparable<E>> implements Iterable<E>, Serializable {

    @SuppressWarnings("rawtypes")
    private static final PersistentPriorityQueue EMPTY = new PersistentPriorityQueue<>(0, null);

    private final int size;
    private final Node<E> root;

    private PersistentPriorityQueue(int size, Node<E> root) {
        this.size = size;
        this.root = root;
    }

    /**
     * Returns an empty PersistentPriorityQueue.
     *
     * @return Empty PersistentPriorityQueue instance
     */
    @SuppressWarnings("unchecked")
    public static <E extends Comparable<E>> PersistentPriorityQueue<E> empty() {
        return (PersistentPriorityQueue<E>) EMPTY;
    }

    /**
     * Purely functional Leftist Heap merge. This is the core operation of the structure.
     * All additions and removals are fundamentally just merges under the hood.
     */
    private static <E extends Comparable<E>> Node<E> mergeNodes(Node<E> h1, Node<E> h2) {
        if (h1 == null) return h2;
        if (h2 == null) return h1;

        // Ensure h1 always holds the smaller root (Min-Heap property)
        if (h1.element.compareTo(h2.element) > 0) {
            Node<E> temp = h1;
            h1 = h2;
            h2 = temp;
        }

        // Recursively merge the right child with the other heap
        Node<E> newRight = mergeNodes(h1.right, h2);

        // Leftist property: the left child must have a rank >= the right child's rank.
        // If the property is violated, we swap the left and right children during recreation.
        if (rank(h1.left) < rank(newRight)) {
            return new Node<>(h1.element, newRight, h1.left);
        } else {
            return new Node<>(h1.element, h1.left, newRight);
        }
    }

    /**
     * O(1) utility to fetch the rank (null path length), safely handling null leaves.
     */
    private static <E> int rank(Node<E> node) {
        return node == null ? 0 : node.rank;
    }

    /**
     * Checks if the priority queue is empty.
     *
     * @return true if the queue contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements in this priority queue.
     *
     * @return the size of the queue
     */
    public int size() {
        return size;
    }

    /**
     * Retrieves, but does not remove, the minimum element of this queue. O(1) time.
     *
     * @return an Optional containing the minimum element, or an empty Optional if the queue is empty
     */
    public Optional<E> peek() {
        return isEmpty() ? Optional.empty() : Optional.of(root.element);
    }

    /**
     * Returns a new PersistentPriorityQueue with the specified element inserted.
     * Path-copying ensures O(log N) structural sharing.
     *
     * @param element the element to add
     * @return a new PersistentPriorityQueue containing the new element
     */
    public PersistentPriorityQueue<E> add(E element) {
        Objects.requireNonNull(element, "PersistentPriorityQueue does not permit null elements");
        Node<E> newNode = new Node<>(element, null, null);
        Node<E> newRoot = mergeNodes(this.root, newNode);
        return new PersistentPriorityQueue<>(this.size + 1, newRoot);
    }

    /**
     * Returns a new PersistentPriorityQueue with the minimum element removed. O(log N) time.
     * If the queue is already empty, returns this same empty instance.
     *
     * @return a new PersistentPriorityQueue without the minimum element
     */
    public PersistentPriorityQueue<E> removeMin() {
        if (isEmpty()) {
            return this; // Avoid allocation
        }
        // Removing the root simply means merging its left and right subtrees
        Node<E> newRoot = mergeNodes(root.left, root.right);
        return new PersistentPriorityQueue<>(this.size - 1, newRoot);
    }

    /**
     * Merges this priority queue with another, returning a new priority queue containing
     * all elements from both. Because this uses a Leftist Heap, merging is an O(log N) operation.
     *
     * @param other the priority queue to merge with
     * @return a new PersistentPriorityQueue containing all combined elements
     */
    public PersistentPriorityQueue<E> merge(PersistentPriorityQueue<E> other) {
        Objects.requireNonNull(other, "Cannot merge with a null queue");
        if (this.isEmpty()) return other;
        if (other.isEmpty()) return this;

        Node<E> newRoot = mergeNodes(this.root, other.root);
        return new PersistentPriorityQueue<>(this.size + other.size, newRoot);
    }

    /**
     * Provides an UNORDERED iterator over the queue's elements.
     * Uses a stack for O(1) amortized iteration.
     * Note: Standard priority queues do not guarantee sorted iteration. To consume
     * elements in sorted order, repeatedly call {@code removeMin()}.
     */
    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Stack<Node<E>> stack = new Stack<>();

            {
                if (root != null) {
                    stack.push(root);
                }
            }

            @Override
            public boolean hasNext() {
                return !stack.isEmpty();
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Node<E> current = stack.pop();
                if (current.right != null) stack.push(current.right);
                if (current.left != null) stack.push(current.left);
                return current.element;
            }
        };
    }

    /**
     * Evaluates equality based on the elements contained, independent of internal heap structure.
     * Because heap shapes can vary based on insertion order, this requires O(N log N) time
     * to sort and compare the elements array.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentPriorityQueue<?> that)) {
            return false;
        }
        if (this.size != that.size) {
            return false;
        }

        Object[] thisElements = this.toArray();
        Object[] thatElements = that.toArray();

        Arrays.sort(thisElements);
        Arrays.sort(thatElements);

        return Arrays.equals(thisElements, thatElements);
    }

    @Override
    public int hashCode() {
        Object[] elements = this.toArray();
        Arrays.sort(elements);
        return Arrays.hashCode(elements);
    }

    private Object[] toArray() {
        Object[] arr = new Object[size];
        int i = 0;
        for (E e : this) {
            arr[i++] = e;
        }
        return arr;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        // Output elements in sorted order for clarity in logs/debugging
        Object[] elements = this.toArray();
        Arrays.sort(elements);
        return Arrays.toString(elements);
    }

    @Serial
    private Object writeReplace() {
        return new SerializationProxy<>(this);
    }

    @Serial
    private void readObject(@SuppressWarnings("unused") java.io.ObjectInputStream stream) throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Serialization proxy required");
    }

    /**
     * Internal node structure for the Leftist Heap.
     * The rank (s-value) tracks the distance to the nearest null node.
     */
    private static final class Node<E> implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        final E element;
        final int rank;
        final Node<E> left;
        final Node<E> right;

        Node(E element, Node<E> left, Node<E> right) {
            this.element = element;
            this.left = left;
            this.right = right;
            // Rank is always 1 + the rank of the right child
            this.rank = 1 + PersistentPriorityQueue.rank(right);
        }
    }

    private record SerializationProxy<E extends Comparable<E>>(Object[] elements) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        private SerializationProxy(PersistentPriorityQueue<E> elements) {
            this(elements.toArray());
        }

        @Serial
        @SuppressWarnings("unchecked")
        private Object readResolve() {
            PersistentPriorityQueue<E> queue = PersistentPriorityQueue.empty();
            for (Object element : elements) {
                queue = queue.add((E) element);
            }
            return queue;
        }
    }
}
