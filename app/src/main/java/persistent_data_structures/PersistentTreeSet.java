package persistent_data_structures;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Stack;

/**
 * An immutable, persistent sorted set based on an AVL tree. Lookups,
 * insertions, and deletions are guaranteed O(log N) worst-case time. This
 * collection does not permit {@code null} elements.
 *
 * @param <E> the type of elements maintained by this set (must be Comparable)
 */
public final class PersistentTreeSet<E extends Comparable<E>> implements Iterable<E>, Serializable {

    @SuppressWarnings("rawtypes")
    private static final PersistentTreeSet EMPTY = new PersistentTreeSet<>(0, null);

    private final int size;
    private final Node<E> root;

    /**
     * Internal node structure for the AVL tree. Immutable by design. The height
     * is cached to ensure O(1) balance factor calculations.
     */
    private static final class Node<E> implements Serializable {

        private static final long serialVersionUID = 1L;

        final E element;
        final int height;
        final Node<E> left;
        final Node<E> right;

        Node(E element, int height, Node<E> left, Node<E> right) {
            this.element = element;
            this.height = height;
            this.left = left;
            this.right = right;
        }
    }

    private PersistentTreeSet(int size, Node<E> root) {
        this.size = size;
        this.root = root;
    }

    /**
     * Returns an empty PersistentTreeSet.
     *
     * @param <E> the type of elements
     * @return Empty PersistentTreeSet instance
     */
    @SuppressWarnings("unchecked")
    public static <E extends Comparable<E>> PersistentTreeSet<E> empty() {
        return (PersistentTreeSet<E>) EMPTY;
    }

    /**
     * Checks if the set is empty.
     *
     * @return true if the set contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements in this set.
     *
     * @return the size of the set
     */
    public int size() {
        return size;
    }

    /**
     * Returns true if this set contains the specified element. O(log N) time.
     *
     * @param element the element whose presence in this set is to be tested
     * @return true if this set contains the specified element
     */
    public boolean contains(E element) {
        Objects.requireNonNull(element, "PersistentTreeSet does not permit null elements");
        Node<E> node = root;
        while (node != null) {
            int cmp = element.compareTo(node.element);
            if (cmp == 0) {
                return true;
            }
            node = cmp < 0 ? node.left : node.right;
        }
        return false;
    }

    /**
     * Returns a new PersistentTreeSet containing the specified element. If the
     * set previously contained the element, the original set instance is
     * returned. Path-copying ensures O(log N) structural sharing.
     *
     * @param element element to be added to the set
     * @return a new PersistentTreeSet containing the element, or the current
     * set if already present
     */
    public PersistentTreeSet<E> add(E element) {
        Objects.requireNonNull(element, "PersistentTreeSet does not permit null elements");

        boolean[] added = new boolean[1];
        Node<E> newRoot = add(root, element, added);

        if (newRoot == root) {
            return this; // No changes made (element already present)
        }

        return new PersistentTreeSet<>(size + 1, newRoot);
    }

    private Node<E> add(Node<E> node, E element, boolean[] added) {
        if (node == null) {
            added[0] = true;
            return new Node<>(element, 1, null, null);
        }

        int cmp = element.compareTo(node.element);
        if (cmp == 0) {
            return node; // Set semantics: ignore duplicate elements entirely
        }

        Node<E> newLeft = node.left;
        Node<E> newRight = node.right;

        if (cmp < 0) {
            newLeft = add(node.left, element, added);
            if (newLeft == node.left) {
                return node; // Propagate unchanged state up the tree
            }
        } else {
            newRight = add(node.right, element, added);
            if (newRight == node.right) {
                return node;
            }
        }

        // Rebalance functionally
        return balance(node.element, newLeft, newRight);
    }

    /**
     * Returns a new PersistentTreeSet with the specified element removed. O(log
     * N) time. Returns the exact same set instance if the element did not
     * exist.
     *
     * @param element element to be removed from the set
     * @return a new PersistentTreeSet without the element, or the current set
     * if not found
     */
    public PersistentTreeSet<E> remove(E element) {
        Objects.requireNonNull(element, "PersistentTreeSet does not permit null elements");

        boolean[] removed = new boolean[1];
        Node<E> newRoot = remove(root, element, removed);

        if (!removed[0]) {
            return this; // Element wasn't found
        }
        return new PersistentTreeSet<>(size - 1, newRoot);
    }

    private Node<E> remove(Node<E> node, E element, boolean[] removed) {
        if (node == null) {
            return null;
        }

        int cmp = element.compareTo(node.element);
        if (cmp < 0) {
            Node<E> newLeft = remove(node.left, element, removed);
            if (newLeft == node.left) {
                return node;
            }
            return balance(node.element, newLeft, node.right);
        } else if (cmp > 0) {
            Node<E> newRight = remove(node.right, element, removed);
            if (newRight == node.right) {
                return node;
            }
            return balance(node.element, node.left, newRight);
        } else {
            // Node to remove found
            removed[0] = true;
            if (node.left == null) {
                return node.right;
            }
            if (node.right == null) {
                return node.left;
            }

            // Node has two children. Find the successor (minimum of right subtree).
            Node<E> successor = min(node.right);
            // Functionally remove the successor from the right subtree.
            Node<E> newRight = removeMin(node.right);
            return balance(successor.element, node.left, newRight);
        }
    }

    private Node<E> min(Node<E> node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private Node<E> removeMin(Node<E> node) {
        if (node.left == null) {
            return node.right;
        }
        return balance(node.element, removeMin(node.left), node.right);
    }

    /**
     * O(1) utility to fetch height, safely handling null leaves.
     */
    private int height(Node<E> node) {
        return node == null ? 0 : node.height;
    }

    /**
     * Combines node creation with height recalculation.
     */
    private Node<E> createNode(E element, Node<E> left, Node<E> right) {
        int h = 1 + Math.max(height(left), height(right));
        return new Node<>(element, h, left, right);
    }

    /**
     * Purely functional AVL balancing. Strictly allocates only the final
     * balanced configuration.
     */
    private Node<E> balance(E element, Node<E> left, Node<E> right) {
        int balanceFactor = height(left) - height(right);

        // Left Heavy
        if (balanceFactor > 1) {
            if (height(left.left) >= height(left.right)) {
                // LL Case
                Node<E> newRight = createNode(element, left.right, right);
                return createNode(left.element, left.left, newRight);
            } else {
                // LR Case
                Node<E> leftRight = left.right;
                Node<E> newRight = createNode(element, leftRight.right, right);
                Node<E> newLeft = createNode(left.element, left.left, leftRight.left);
                return createNode(leftRight.element, newLeft, newRight);
            }
        } // Right Heavy
        else if (balanceFactor < -1) {
            if (height(right.right) >= height(right.left)) {
                // RR Case
                Node<E> newLeft = createNode(element, left, right.left);
                return createNode(right.element, newLeft, right.right);
            } else {
                // RL Case
                Node<E> rightLeft = right.left;
                Node<E> newLeft = createNode(element, left, rightLeft.left);
                Node<E> newRight = createNode(right.element, rightLeft.right, right.right);
                return createNode(rightLeft.element, newLeft, newRight);
            }
        }

        // Already balanced
        return createNode(element, left, right);
    }

    /**
     * Provides an in-order iterator over the set's elements. Uses a stateful
     * stack internally to suspend/resume traversal, ensuring O(1) amortized
     * next() execution and O(log N) memory overhead.
     */
    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Stack<Node<E>> stack = new Stack<>();

            {
                pushLeft(root);
            }

            private void pushLeft(Node<E> node) {
                while (node != null) {
                    stack.push(node);
                    node = node.left;
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
                pushLeft(current.right);
                return current.element;
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentTreeSet)) {
            return false;
        }

        PersistentTreeSet<?> that = (PersistentTreeSet<?>) o;
        if (this.size != that.size) {
            return false;
        }

        Iterator<E> thisIterator = this.iterator();
        Iterator<?> thatIterator = that.iterator();

        while (thisIterator.hasNext()) {
            E thisElement = thisIterator.next();
            Object thatElement = thatIterator.next();

            if (!Objects.equals(thisElement, thatElement)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the sum of the hash codes of the elements in this set, fulfilling
     * the general contract of Set.hashCode().
     */
    @Override
    public int hashCode() {
        int h = 0;
        for (E element : this) {
            h += element.hashCode();
        }
        return h;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[");
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private Object writeReplace() {
        return new SerializationProxy<>(this);
    }

    private void readObject(@SuppressWarnings("unused") java.io.ObjectInputStream stream) throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Serialization proxy required");
    }

    private static class SerializationProxy<E extends Comparable<E>> implements Serializable {

        private static final long serialVersionUID = 1L;
        private final Object[] elements;

        SerializationProxy(PersistentTreeSet<E> set) {
            this.elements = new Object[set.size()];
            int i = 0;
            for (E element : set) {
                this.elements[i++] = element;
            }
        }

        @SuppressWarnings("unchecked")
        private Object readResolve() {
            PersistentTreeSet<E> set = PersistentTreeSet.empty();
            for (Object element : elements) {
                set = set.add((E) element);
            }
            return set;
        }
    }
}
