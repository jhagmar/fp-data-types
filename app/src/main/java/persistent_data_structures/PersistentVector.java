package persistent_data_structures;

import annotations.ExcludeFromCoverageGeneratedReport;
import type_support.Hasher;
import type_support.StandardOrderedHasher;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * An immutable, persistent vector based on a 32-way bit-partitioned radix tree.
 * Appends and updates are amortized O(1) (effectively O(log32 N)). This
 * collection does not permit {@code null} elements.
 *
 * @param <T> the type of elements in this vector
 */
public final class PersistentVector<T> implements Iterable<T>, Serializable {

    // The tree branches 32 ways at each level. 32 is 2^5. 
    // Therefore, every 5 bits of an index determines the path at a specific depth.
    private static final int BIT_WIDTH = 5;
    private static final int NODE_SIZE = 1 << BIT_WIDTH; // 32
    private static final int BIT_MASK = NODE_SIZE - 1;   // 31 (Binary: 11111)

    private static final Node EMPTY_NODE = new Node(new Object[NODE_SIZE]);
    private static final Object[] EMPTY_ARRAY = new Object[0];
    @SuppressWarnings("rawtypes")
    private static final PersistentVector EMPTY = new PersistentVector<>(0, BIT_WIDTH, EMPTY_NODE, EMPTY_ARRAY);
    
    private final int size;
    private final int shift; // Defines the current depth of the tree. E.g., shift=5 means depth 1.
    private final Node root;
    private final Object[] tail; // Holds up to the last 32 elements for fast O(1) appends.

    private PersistentVector(int size, int shift, Node root, Object[] tail) {
        this.size = size;
        this.shift = shift;
        this.root = root;
        this.tail = tail;
    }

    /**
     * Returns an empty PersistentVector.
     *
     * @return Empty PersistentVector instance
     */
    @SuppressWarnings("unchecked")
    public static <T> PersistentVector<T> empty() {
        return (PersistentVector<T>) EMPTY;
    }

    /**
     * Creates a PersistentVector containing the given elements.
     *
     * @param elements the elements to include in the vector
     * @return a PersistentVector containing the provided elements
     */
    @SafeVarargs
    public static <T> PersistentVector<T> of(T... elements) {
        PersistentVector<T> result = empty();
        for (T element : elements) {
            result = result.append(element);
        }
        return result;
    }

    /**
     * Checks if the vector is empty.
     *
     * @return true if the vector contains no elements, false otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements in the vector.
     *
     * @return the size of the vector
     */
    public int size() {
        return size;
    }

    /**
     * Calculates the starting index of the tail array. If size is 35, the tail
     * holds elements 32, 33, 34. Tail offset is 32.
     */
    private int tailOffset() {
        if (size < NODE_SIZE) {
            return 0; // Everything is in the tail
        }
        // Drops the remainder to find the nearest multiple of 32.
        return ((size - 1) >>> BIT_WIDTH) << BIT_WIDTH;
    }

    /**
     * Retrieves the element at the specified index. O(log32 N) due to tree traversal.
     *
     * @param index the index of the element to retrieve
     * @return the element at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size)
     */
    @SuppressWarnings("unchecked")
    public T get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        // arrayFor(index) gets the 32-element array containing our item.
        // index & BIT_MASK gets the exact position (0-31) within that chunk.
        return (T) arrayFor(index)[index & BIT_MASK];
    }

    /**
     * Returns a new PersistentVector with the element at the specified index replaced
     * by the given element. O(log32 N) due to tree traversal and path copying.
     *
     * @param index   the index of the element to replace
     * @param element the new element to set at the specified index
     * @return a new PersistentVector with the updated element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= size)
     */
    public PersistentVector<T> set(int index, T element) {
        Objects.requireNonNull(element, "PersistentVector does not permit null elements");
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }

        // If the element belongs in the tail, we only need to clone the tail O(1)
        if (index >= tailOffset()) {
            Object[] newTail = tail.clone();
            newTail[index & BIT_MASK] = element;
            return new PersistentVector<>(size, shift, root, newTail);
        }

        // Otherwise, it's in the tree. We must "path copy" down to the element.
        return new PersistentVector<>(size, shift, setNode(shift, root, index, element), tail);
    }

    /**
     * Recursively walks down the tree, cloning nodes along the path to the
     * target index. This creates a new version of the tree without mutating the
     * old one.
     */
    private Node setNode(int level, Node node, int index, T element) {
        Node ret = new Node(node.array.clone());
        if (level == 0) {
            // We've reached the leaf. Put the element in.
            ret.array[index & BIT_MASK] = element;
        } else {
            // We are at an internal node.
            // 'index >>> level' shifts out the lower bits to figure out which child branch to take.
            int subIndex = (index >>> level) & BIT_MASK;
            ret.array[subIndex] = setNode(level - BIT_WIDTH, (Node) node.array[subIndex], index, element);
        }
        return ret;
    }

    /**
     * Returns a new PersistentVector with the given element appended to the end. O(1) amortized.
     *
     * @param element the element to append
     * @return a new PersistentVector with the element appended
     * @throws NullPointerException if the element is null, as PersistentVector does not permit null elements
     */
    public PersistentVector<T> append(T element) {
        Objects.requireNonNull(element, "PersistentVector does not permit null elements");

        // Fast path: Tail isn't full yet. Clone tail, append, return. O(1)
        if (tail.length < NODE_SIZE) {
            Object[] newTail = Arrays.copyOf(tail, tail.length + 1);
            newTail[tail.length] = element;
            return new PersistentVector<>(size + 1, shift, root, newTail);
        }

        // Slow path: Tail is full. We must package it as a leaf node and push it into the tree.
        Node tailNode = new Node(tail);
        int newShift = shift;
        Node newRoot;

        // Check if the current tree is completely full.
        // If the number of full nodes exceeds what the current depth (shift) can hold, we overflow.
        if ((size >>> BIT_WIDTH) > (1 << shift)) {
            // Overflow - We must grow the tree upwards. Create a new root.
            newRoot = new Node(new Object[NODE_SIZE]);
            newRoot.array[0] = root; // Old tree becomes the left-most branch
            newRoot.array[1] = newPath(shift, tailNode); // New tail gets its own deep branch on the right
            newShift += BIT_WIDTH; // Increase depth
        } else {
            // Tree is not full, just weave the tail node into the existing right-most edge.
            newRoot = pushTail(shift, root, tailNode);
        }

        // Start a brand new, empty tail with the appended element
        Object[] newTail = new Object[1];
        newTail[0] = element;
        return new PersistentVector<>(size + 1, newShift, newRoot, newTail);
    }

    /**
     * Creates a vertical sequence of nodes down to a leaf. Used when growing
     * the tree.
     */
    private Node newPath(int level, Node node) {
        if (level == 0) {
            return node;
        }
        Node ret = new Node(new Object[NODE_SIZE]);
        ret.array[0] = newPath(level - BIT_WIDTH, node);
        return ret;
    }

    /**
     * Pushes a full tail node into the right-most available slot in the tree.
     */
    private Node pushTail(int level, Node parent, Node tailNode) {
        // Find where the last node should logically go based on current size
        int subIndex = ((size - 1) >>> level) & BIT_MASK;
        Node ret = new Node(parent.array.clone());

        if (level == BIT_WIDTH) {
            // We are one level above the leaves. Attach the tail directly.
            ret.array[subIndex] = tailNode;
        } else {
            Node child = (Node) parent.array[subIndex];
            if (child == null) {
                // The branch doesn't exist yet, build it out.
                ret.array[subIndex] = newPath(level - BIT_WIDTH, tailNode);
            } else {
                // The branch exists, recurse down to find the empty slot.
                ret.array[subIndex] = pushTail(level - BIT_WIDTH, child, tailNode);
            }
        }
        return ret;
    }

    /**
     * Returns a new PersistentVector with the last element removed. O(1) amortized.
     *
     * @return a new PersistentVector with the last element removed
     * @throws IllegalStateException if the vector is empty, as you cannot pop from an empty vector
     */
    public PersistentVector<T> pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot pop from an empty vector");
        }
        if (size == 1) {
            return empty();
        }

        // Fast path: Tail has more than 1 item. Just shrink the tail. O(1)
        if (tail.length > 1) {
            Object[] newTail = Arrays.copyOf(tail, tail.length - 1);
            return new PersistentVector<>(size - 1, shift, root, newTail);
        }

        // Slow path: Tail is empty after pop. We must steal the right-most leaf from the tree
        // to become our new tail. (size - 2 because size is 1-based, and we want the index before the old tail).
        Object[] newTail = arrayFor(size - 2);
        Node newRoot = popTail(shift, root);
        int newShift = shift;

        // Contract the tree: if popping emptied out the entire right side of the root,
        // the root is redundant. We can drop it and make its only child the new root.
        if (shift > BIT_WIDTH && newRoot.array[1] == null) {
            newRoot = (Node) newRoot.array[0];
            newShift -= BIT_WIDTH;
        }

        if (newRoot == null) {
            newRoot = EMPTY_NODE;
        }

        return new PersistentVector<>(size - 1, newShift, newRoot, newTail);
    }

    private Node popTail(int level, Node node) {
        int subIndex = ((size - 2) >>> level) & BIT_MASK;
        if (level > BIT_WIDTH) {
            Node newChild = popTail(level - BIT_WIDTH, (Node) node.array[subIndex]);
            if (newChild == null && subIndex == 0) {
                return null; // Branch became empty, bubble up the null
            } else {
                Node ret = new Node(node.array.clone());
                ret.array[subIndex] = newChild;
                return ret;
            }
        } else if (subIndex == 0) {
            return null; // Leaf became empty, signal parent to remove it
        } else {
            Node ret = new Node(node.array.clone());
            ret.array[subIndex] = null;
            return ret;
        }
    }

    /**
     * Traverses the tree to find the 32-element array chunk that contains the
     * given index.
     */
    private Object[] arrayFor(int index) {
        if (index >= tailOffset()) {
            return tail;
        }

        Node node = root;
        // Shift drops by 5 on each step, stripping off 5 bits from the index to act as directions.
        for (int level = shift; level > 0; level -= BIT_WIDTH) {
            node = (Node) node.array[(index >>> level) & BIT_MASK];
        }
        return node.array;
    }

    /**
     * Converts this PersistentVector to a standard Java List. The returned list is
     * unmodifiable and reflects the state of the vector at the time of conversion. O(N) time.
     *
     * @return an unmodifiable List containing the elements of this PersistentVector
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        for (T element : this) {
            result.add(element);
        }
        return java.util.Collections.unmodifiableList(result);
    }

    @Serial
    private Object writeReplace() {
        return new SerializationProxy<>(this);
    }

    @Serial
    @ExcludeFromCoverageGeneratedReport
    private void readObject(@SuppressWarnings("unused") java.io.ObjectInputStream stream) throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Serialization proxy required");
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private int index = 0;
            private int base = 0; // Tracks the starting index of the current 32-element chunk
            private Object[] currentBlock = (size > 0) ? arrayFor(0) : null;

            @Override
            public boolean hasNext() {
                return index < size;
            }

            @Override
            @SuppressWarnings("unchecked")
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No such element");
                }

                // If we've exhausted this 32-element chunk, fetch the next one.
                if (index - base == NODE_SIZE) {
                    base += NODE_SIZE;
                    currentBlock = arrayFor(base);
                }
                return (T) currentBlock[index++ - base];
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentVector<?> that)) {
            return false;
        }

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
     * Internal node structure for the radix tree. In a leaf node (level 0), the
     * array holds the actual elements (T). In an internal node (level > 0), the
     * array holds references to child Nodes.
     */
    private static final class Node implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        final Object[] array;

        Node(Object[] array) {
            this.array = array;
        }
    }

    private static class SerializationProxy<T> implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private final Object[] elements;

        SerializationProxy(PersistentVector<T> vector) {
            this.elements = new Object[vector.size()];
            int i = 0;
            for (T element : vector) {
                this.elements[i++] = element;
            }
        }

        @Serial
        @SuppressWarnings("unchecked")
        private Object readResolve() {
            return PersistentVector.of((T[]) elements);
        }
    }
}
