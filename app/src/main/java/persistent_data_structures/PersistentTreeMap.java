package persistent_data_structures;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * An immutable, persistent sorted map based on an AVL tree. Lookups,
 * insertions, and deletions are guaranteed O(log N) worst-case time. This
 * collection does not permit {@code null} keys or values.
 *
 * @param <K> the type of keys maintained by this map (must be Comparable)
 * @param <V> the type of mapped values
 */
public final class PersistentTreeMap<K extends Comparable<K>, V> implements Iterable<Map.Entry<K, V>>, Serializable {

    @SuppressWarnings("rawtypes")
    private static final PersistentTreeMap EMPTY = new PersistentTreeMap<>(0, null);

    private final int size;
    private final Node<K, V> root;

    private PersistentTreeMap(int size, Node<K, V> root) {
        this.size = size;
        this.root = root;
    }

    /**
     * Returns an empty PersistentTreeMap.
     *
     * @return Empty PersistentTreeMap instance
     */
    @SuppressWarnings("unchecked")
    public static <K extends Comparable<K>, V> PersistentTreeMap<K, V> empty() {
        return (PersistentTreeMap<K, V>) EMPTY;
    }

    /**
     * Checks if the map is empty.
     *
     * @return true if the map contains no key-value mappings
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of key-value mappings in this map.
     *
     * @return the size of the map
     */
    public int size() {
        return size;
    }

    /**
     * Retrieves the value to which the specified key is mapped, or null if this
     * map contains no mapping for the key. O(log N) time.
     *
     * @param key the key whose associated value is to be returned
     * @return an Optional containing the value to which the specified key is
     * mapped, or an empty Optional if this map contains no mapping for the key
     */
    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "PersistentTreeMap does not permit null keys");
        Node<K, V> node = root;
        while (node != null) {
            int cmp = key.compareTo(node.key);
            if (cmp == 0) {
                return Optional.of(node.value);
            }
            node = cmp < 0 ? node.left : node.right;
        }
        return Optional.empty();
    }

    /**
     * Returns true if this map contains a mapping for the specified key.
     *
     * @param key the key whose presence in this map is to be tested
     * @return true if this map contains a mapping for the specified key
     */
    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "PersistentTreeMap does not permit null keys");
        Node<K, V> node = root;
        while (node != null) {
            int cmp = key.compareTo(node.key);
            if (cmp == 0) {
                return true;
            }
            node = cmp < 0 ? node.left : node.right;
        }
        return false;
    }

    /**
     * Returns a new PersistentTreeMap with the specified key-value mapping. If
     * the map previously contained a mapping for the key, the old value is
     * replaced. Path-copying ensures O(log N) structural sharing.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return a new PersistentTreeMap containing the updated mapping
     */
    public PersistentTreeMap<K, V> put(K key, V value) {
        Objects.requireNonNull(key, "PersistentTreeMap does not permit null keys");
        Objects.requireNonNull(value, "PersistentTreeMap does not permit null values");

        // We use an array of size 1 as a mutable reference to track if the size
        // actually increased during the recursive put, avoiding a full tree traversal
        // to recalculate size.
        boolean[] added = new boolean[1];
        Node<K, V> newRoot = put(root, key, value, added);

        if (newRoot == root) {
            return this; // No changes made (key already mapped to exact same value)
        }

        int newSize = added[0] ? size + 1 : size;
        return new PersistentTreeMap<>(newSize, newRoot);
    }

    private Node<K, V> put(Node<K, V> node, K key, V value, boolean[] added) {
        if (node == null) {
            added[0] = true;
            return new Node<>(key, value, 1, null, null);
        }

        int cmp = key.compareTo(node.key);
        if (cmp == 0) {
            if (Objects.equals(value, node.value)) {
                return node; // Optimization: Identity returned if no state changes
            }
            return new Node<>(key, value, node.height, node.left, node.right);
        }

        Node<K, V> newLeft = node.left;
        Node<K, V> newRight = node.right;

        if (cmp < 0) {
            newLeft = put(node.left, key, value, added);
            if (newLeft == node.left) {
                return node; // Propagate unchanged state up the tree
            }
        } else {
            newRight = put(node.right, key, value, added);
            if (newRight == node.right) {
                return node;
            }
        }

        // Rebalance the node functionally. Allocates a new node only after verifying balance.
        return balance(node.key, node.value, newLeft, newRight);
    }

    /**
     * Returns a new PersistentTreeMap with the mapping for a key removed. O(log
     * N) time. Returns the exact same map instance if the key did not exist.
     *
     * @param key key whose mapping is to be removed from the map
     * @return a new PersistentTreeMap without the mapping
     */
    public PersistentTreeMap<K, V> remove(K key) {
        Objects.requireNonNull(key, "PersistentTreeMap does not permit null keys");

        boolean[] removed = new boolean[1];
        Node<K, V> newRoot = remove(root, key, removed);

        if (!removed[0]) {
            return this; // Key wasn't found, avoid allocation and return current instance
        }
        return new PersistentTreeMap<>(size - 1, newRoot);
    }

    private Node<K, V> remove(Node<K, V> node, K key, boolean[] removed) {
        if (node == null) {
            return null;
        }

        int cmp = key.compareTo(node.key);
        if (cmp < 0) {
            Node<K, V> newLeft = remove(node.left, key, removed);
            if (newLeft == node.left) {
                return node;
            }
            return balance(node.key, node.value, newLeft, node.right);
        } else if (cmp > 0) {
            Node<K, V> newRight = remove(node.right, key, removed);
            if (newRight == node.right) {
                return node;
            }
            return balance(node.key, node.value, node.left, newRight);
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
            Node<K, V> successor = min(node.right);
            // Functionally remove the successor from the right subtree.
            Node<K, V> newRight = removeMin(node.right);
            return balance(successor.key, successor.value, node.left, newRight);
        }
    }

    private Node<K, V> min(Node<K, V> node) {
        while (node.left != null) {
            node = node.left;
        }
        return node;
    }

    private Node<K, V> removeMin(Node<K, V> node) {
        if (node.left == null) {
            return node.right;
        }
        return balance(node.key, node.value, removeMin(node.left), node.right);
    }

    /**
     * O(1) utility to fetch height, safely handling null leaves.
     */
    private int height(Node<K, V> node) {
        return node == null ? 0 : node.height;
    }

    /**
     * Combines node creation with height recalculation.
     */
    private Node<K, V> createNode(K key, V value, Node<K, V> left, Node<K, V> right) {
        int h = 1 + Math.max(height(left), height(right));
        return new Node<>(key, value, h, left, right);
    }

    /**
     * Purely functional AVL balancing. Instead of mutating a node or
     * instantiating an unbalanced node just to immediately deconstruct it for a
     * rotation, this function takes the raw components, determines the
     * necessary rotation, and strictly allocates only the final balanced
     * configuration.
     */
    private Node<K, V> balance(K key, V value, Node<K, V> left, Node<K, V> right) {
        int balanceFactor = height(left) - height(right);

        // Left Heavy
        if (balanceFactor > 1) {
            if (height(left.left) >= height(left.right)) {
                // LL Case (Right Rotation)
                Node<K, V> newRight = createNode(key, value, left.right, right);
                return createNode(left.key, left.value, left.left, newRight);
            } else {
                // LR Case (Left Rotation on left child, then Right Rotation)
                Node<K, V> leftRight = left.right;
                Node<K, V> newRight = createNode(key, value, leftRight.right, right);
                Node<K, V> newLeft = createNode(left.key, left.value, left.left, leftRight.left);
                return createNode(leftRight.key, leftRight.value, newLeft, newRight);
            }
        } // Right Heavy
        else if (balanceFactor < -1) {
            if (height(right.right) >= height(right.left)) {
                // RR Case (Left Rotation)
                Node<K, V> newLeft = createNode(key, value, left, right.left);
                return createNode(right.key, right.value, newLeft, right.right);
            } else {
                // RL Case (Right Rotation on right child, then Left Rotation)
                Node<K, V> rightLeft = right.left;
                Node<K, V> newLeft = createNode(key, value, left, rightLeft.left);
                Node<K, V> newRight = createNode(right.key, right.value, rightLeft.right, right.right);
                return createNode(rightLeft.key, rightLeft.value, newLeft, newRight);
            }
        }

        // Already balanced
        return createNode(key, value, left, right);
    }

    /**
     * Provides an in-order iterator over the map's entries. Uses a stateful
     * stack internally to suspend/resume traversal, ensuring O(1) amortized
     * next() execution and O(log N) memory overhead.
     */
    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new Iterator<Map.Entry<K, V>>() {
            private final Stack<Node<K, V>> stack = new Stack<>();

            {
                pushLeft(root);
            }

            private void pushLeft(Node<K, V> node) {
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
            public Map.Entry<K, V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Node<K, V> current = stack.pop();
                pushLeft(current.right);
                return new AbstractMap.SimpleImmutableEntry<>(current.key, current.value);
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentTreeMap<?, ?> that)) {
            return false;
        }

        if (this.size != that.size) {
            return false;
        }

        Iterator<Map.Entry<K, V>> thisIterator = this.iterator();
        Iterator<? extends Map.Entry<?, ?>> thatIterator = that.iterator();

        while (thisIterator.hasNext()) {
            Map.Entry<K, V> thisEntry = thisIterator.next();
            Map.Entry<?, ?> thatEntry = thatIterator.next();

            if (!Objects.equals(thisEntry.getKey(), thatEntry.getKey())
                    || !Objects.equals(thisEntry.getValue(), thatEntry.getValue())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (Map.Entry<K, V> entry : this) {
            h += entry.getKey().hashCode() ^ (entry.getValue().hashCode());
        }
        return h;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        Iterator<Map.Entry<K, V>> it = iterator();
        while (it.hasNext()) {
            Map.Entry<K, V> entry = it.next();
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            if (it.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
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
     * Internal node structure for the AVL tree. Immutable by design. The height
     * is cached to ensure O(1) balance factor calculations.
     */
    private static final class Node<K, V> implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        final K key;
        final V value;
        final int height;
        final Node<K, V> left;
        final Node<K, V> right;

        Node(K key, V value, int height, Node<K, V> left, Node<K, V> right) {
            this.key = key;
            this.value = value;
            this.height = height;
            this.left = left;
            this.right = right;
        }
    }

    private static class SerializationProxy<K extends Comparable<K>, V> implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private final Object[] keys;
        private final Object[] values;

        SerializationProxy(PersistentTreeMap<K, V> map) {
            this.keys = new Object[map.size()];
            this.values = new Object[map.size()];
            int i = 0;
            for (Map.Entry<K, V> entry : map) {
                this.keys[i] = entry.getKey();
                this.values[i] = entry.getValue();
                i++;
            }
        }

        @Serial
        @SuppressWarnings("unchecked")
        private Object readResolve() {
            PersistentTreeMap<K, V> map = PersistentTreeMap.empty();
            for (int i = 0; i < keys.length; i++) {
                map = map.put((K) keys[i], (V) values[i]);
            }
            return map;
        }
    }
}
