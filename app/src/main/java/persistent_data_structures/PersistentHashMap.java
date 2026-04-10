package persistent_data_structures;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

/**
 * An immutable, persistent hash map based on a Hash Array Mapped Trie (HAMT).
 * Lookups, insertions, and deletions are effectively O(1) time (technically
 * O(log32 N)). This collection does not permit {@code null} keys or values.
 * <p>
 * A HAMT uses the 32-bit hash code of a key to navigate a trie. At each level,
 * it consumes 5 bits of the hash to find a path (yielding 2^5 = 32 possible
 * branches). To save memory, sparse arrays are compressed using a bitmap (an
 * {@code int}). A set bit indicates a child exists at that index, and
 * {@code Integer.bitCount()} maps the sparse 32-wide index into a dense array
 * index.
 * <p>
 * When modified, the map does not change in place. Instead, it copies the nodes
 * along the path to the mutated element (Path Copying), sharing the rest of the
 * untouched tree with the older versions of the map.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public final class PersistentHashMap<K, V> implements Iterable<Map.Entry<K, V>>, Serializable {

    // Singleton instance for the empty map to avoid unnecessary object creation.
    @SuppressWarnings("rawtypes")
    private static final PersistentHashMap EMPTY = new PersistentHashMap<>(0, EmptyNode.INSTANCE);

    private static final int BITS_PER_LEVEL = 5;

    // 32 possible branches per node
    private static final int BRANCHING_FACTOR = 1 << BITS_PER_LEVEL;

    // Mask to extract 5 bits (binary 11111, decimal 31)
    private static final int LEVEL_MASK = BRANCHING_FACTOR - 1;

    // Transient fields: Java's default serialization is bypassed via writeReplace()
    private final transient int size;
    private final transient Node<K, V> root;

    private PersistentHashMap(int size, Node<K, V> root) {
        this.size = size;
        this.root = root;
    }

    /**
     * Returns an empty PersistentHashMap.
     *
     * @return Empty PersistentHashMap instance
     */
    @SuppressWarnings("unchecked")
    public static <K, V> PersistentHashMap<K, V> empty() {
        return (PersistentHashMap<K, V>) EMPTY;
    }

    /**
     * Re-hashes the standard Java hashCode using the MurmurHash3 32-bit
     * finalizer. This guarantees a good avalanche effect, ensuring a uniform
     * distribution across the HAMT branches.
     */
    private static int hash(Object key) {
        int h = key.hashCode();

        // MurmurHash3 32-bit finalizer (fmix32)
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;

        return h;
    }

    /**
     * Isolates the 5 bits for the current level (shift) and converts it into a
     * bitmask. E.g., if the 5 bits represent the number 3, this returns 1000
     * (binary).
     */
    private static int bitpos(int hash, int shift) {
        return 1 << ((hash >>> shift) & LEVEL_MASK);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Node<K, V> emptyNode() {
        return (Node<K, V>) EmptyNode.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Node<K, V> mergeLeaves(int shift, Node<K, V> n1, Node<K, V> n2) {
        int h1 = getHash(n1);
        int h2 = getHash(n2);

        if (h1 == h2) {
            return new CollisionNode<>(h1, new LeafNode[]{(LeafNode<K, V>) n1, (LeafNode<K, V>) n2});
        }

        int bit1 = bitpos(h1, shift);
        int bit2 = bitpos(h2, shift);

        if (bit1 == bit2) {
            Node<K, V> child = mergeLeaves(shift + BITS_PER_LEVEL, n1, n2);
            return new BitmapIndexedNode<>(bit1, new Node[]{child});
        } else {
            // Use unsigned comparison so the 31st bit (0x80000000) is treated as the maximum value
            return new BitmapIndexedNode<>(bit1 | bit2,
                    Integer.compareUnsigned(bit1, bit2) < 0 ? new Node[]{n1, n2} : new Node[]{n2, n1});
        }
    }

    private static int getHash(Node<?, ?> node) {
        Objects.requireNonNull(node, "Node cannot be null when calculating hash");
        if (node instanceof LeafNode) {
            return ((LeafNode<?, ?>) node).hash;
        }
        if (node instanceof CollisionNode) {
            return ((CollisionNode<?, ?>) node).hash;
        }
        throw new IllegalStateException("Unexpected node type in merge: " + node.getClass().getSimpleName());
    }

    /**
     * Checks if the map is empty.
     *
     * @return {@code true} if the map is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of key-value mappings in the map.
     *
     * @return the number of key-value mappings in the map
     */
    public int size() {
        return size;
    }

    /**
     * Retrieves the value associated with the specified key, if it exists.
     *
     * @param key the key whose associated value is to be returned
     * @return an {@code Optional} containing the value if found, or empty if
     * not found
     * @throws NullPointerException if the specified key is null
     */
    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "PersistentHashMap does not permit null keys");
        return root.get(0, hash(key), key);
    }

    /**
     * Checks if the map contains the specified key.
     *
     * @param key the key whose presence in the map is to be tested
     * @return {@code true} if the map contains the key, {@code false} otherwise
     * @throws NullPointerException if the specified key is null
     */
    public boolean containsKey(K key) {
        return get(key).isPresent();
    }

    /**
     * Associates the specified value with the specified key in the map.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return a new PersistentHashMap instance with the specified key-value
     * mapping added
     * @throws NullPointerException if the specified key or value is null
     */
    public PersistentHashMap<K, V> put(K key, V value) {
        Objects.requireNonNull(key, "PersistentHashMap does not permit null keys");
        Objects.requireNonNull(value, "PersistentHashMap does not permit null values");

        // Use a boolean array to pass-by-reference whether a new key was actually added
        // (as opposed to an existing key being updated), allowing us to track the total size.
        boolean[] added = new boolean[1];
        Node<K, V> newRoot = root.put(0, hash(key), key, value, added);

        if (newRoot == root) {
            return this; // No changes made (key-value pair already existed)
        }
        return new PersistentHashMap<>(added[0] ? size + 1 : size, newRoot);
    }

    /**
     * Removes the mapping for the specified key from the map if present.
     *
     * @param key the key whose mapping is to be removed from the map
     * @return a new PersistentHashMap instance with the specified key removed,
     * or the same instance if the key was not found
     * @throws NullPointerException if the specified key is null
     */
    public PersistentHashMap<K, V> remove(K key) {
        Objects.requireNonNull(key, "PersistentHashMap does not permit null keys");

        boolean[] removed = new boolean[1];
        Node<K, V> newRoot = root.remove(0, hash(key), key, removed);

        if (!removed[0]) {
            return this; // Key was not found
        }
        return new PersistentHashMap<>(size - 1, newRoot);
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return new Iterator<Map.Entry<K, V>>() {
            // Max depth of a 32-bit hash processed 5 bits at a time is ceil(32/5) = 7.
            // Using a fixed size array of 8 bypasses the overhead of java.util.Stack.
            private final Node<K, V>[] nodePath = new Node[8];
            private final int[] indexPath = new int[8];
            private int depth = -1;

            private LeafNode<K, V> nextLeaf;

            // State for fully iterating through Hash Collisions
            private CollisionNode<K, V> currentCollision;
            private int collisionIdx;

            {
                if (root != EmptyNode.INSTANCE) {
                    depth = 0;
                    nodePath[0] = root;
                    indexPath[0] = 0;
                    advance();
                }
            }

            private void advance() {
                // Drain current collision node if we are inside one
                if (currentCollision != null && collisionIdx < currentCollision.leaves.length) {
                    nextLeaf = currentCollision.leaves[collisionIdx++];
                    return;
                }
                currentCollision = null; // Clear out of collision state

                while (depth >= 0) {
                    Node<K, V> node = nodePath[depth];
                    int idx = indexPath[depth];

                    switch (node) {
                        case LeafNode<K, V> leafNode -> {
                            nextLeaf = leafNode;
                            depth--; // Step back up

                            return; // Step back up
                        }
                        case CollisionNode<K, V> collisionNode -> {
                            currentCollision = collisionNode;
                            collisionIdx = 1; // Prepare the iterator to pick up at index 1 next time

                            nextLeaf = currentCollision.leaves[0];
                            depth--;
                            return;
                        }
                        case BitmapIndexedNode<K, V> bin -> {
                            if (idx < bin.nodes.length) {
                                indexPath[depth]++; // Increment index for when we backtrack to this node

                                // Push child to the path stack
                                depth++;
                                nodePath[depth] = bin.nodes[idx];
                                indexPath[depth] = 0;
                            } else {
                                depth--; // Exhausted this node, step back up
                            }
                        }
                        default -> {
                            Objects.requireNonNull(node, "Node cannot be null during iteration");
                            throw new IllegalStateException("Unknown node type in iterator: " + node.getClass().getSimpleName());
                        }
                    }
                }
                nextLeaf = null; // Exhausted the entire tree
            }

            @Override
            public boolean hasNext() {
                return nextLeaf != null;
            }

            @Override
            public Map.Entry<K, V> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                Map.Entry<K, V> entry = new AbstractMap.SimpleImmutableEntry<>(nextLeaf.key, nextLeaf.value);
                advance();
                return entry;
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentHashMap)) {
            return false;
        }

        PersistentHashMap<K, V> that = (PersistentHashMap<K, V>) o;
        if (this.size != that.size) {
            return false;
        }

        for (Map.Entry<K, V> entry : this) {
            Optional<V> thatValue = that.get(entry.getKey());
            if (thatValue.isEmpty() || !Objects.equals(entry.getValue(), thatValue.get())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int h = 0;
        // HAMT traversal order is non-deterministic relative to insertion,
        // so the hash function must be commutative (like addition).
        for (Map.Entry<K, V> entry : this) {
            h += entry.getKey().hashCode() ^ entry.getValue().hashCode();
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
        return sb.append("}").toString();
    }

    @Serial
    private Object writeReplace() {
        return new SerializationProxy<>(this);
    }

    @Serial
    private void readObject(@SuppressWarnings("unused") java.io.ObjectInputStream stream) throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Serialization proxy required");
    }

    // Nodes do not implement Serializable, as the Proxy handles all persistence.
    private interface Node<K, V> {

        Optional<V> get(int shift, int hash, K key);

        Node<K, V> put(int shift, int hash, K key, V value, boolean[] added);

        Node<K, V> remove(int shift, int hash, K key, boolean[] removed);
    }

    private static final class EmptyNode<K, V> implements Node<K, V> {

        @SuppressWarnings("rawtypes")
        static final EmptyNode INSTANCE = new EmptyNode();

        @Override
        public Optional<V> get(int shift, int hash, K key) {
            return Optional.empty();
        }

        @Override
        public Node<K, V> put(int shift, int hash, K key, V value, boolean[] added) {
            added[0] = true;
            return new LeafNode<>(hash, key, value);
        }

        @Override
        public Node<K, V> remove(int shift, int hash, K key, boolean[] removed) {
            return this;
        }
    }

    private static final class LeafNode<K, V> implements Node<K, V> {

        final int hash;
        final K key;
        final V value;

        LeafNode(int hash, K key, V value) {
            this.hash = hash;
            this.key = key;
            this.value = value;
        }

        @Override
        public Optional<V> get(int shift, int h, K k) {
            return (this.hash == h && Objects.equals(this.key, k)) ? Optional.of(value) : Optional.empty();
        }

        @Override
        public Node<K, V> put(int shift, int h, K k, V v, boolean[] added) {
            if (this.hash == h && Objects.equals(this.key, k)) {
                if (Objects.equals(this.value, v)) {
                    return this; // Update is identical, return existing node
                }
                return new LeafNode<>(h, k, v); // Replace value
            }
            added[0] = true;
            // Key is different; merge the existing leaf and the new leaf into a branching node
            return mergeLeaves(shift, this, new LeafNode<>(h, k, v));
        }

        @Override
        public Node<K, V> remove(int shift, int h, K k, boolean[] removed) {
            if (this.hash == h && Objects.equals(this.key, k)) {
                removed[0] = true;
                return emptyNode();
            }
            return this;
        }
    }

    /**
     * Handles the rare case where two distinct keys evaluate to the exact same
     * 32-bit hash. Instead of branching, they are stored linearly in an array.
     */
    private static final class CollisionNode<K, V> implements Node<K, V> {

        final int hash;
        final LeafNode<K, V>[] leaves;

        CollisionNode(int hash, LeafNode<K, V>[] leaves) {
            this.hash = hash;
            this.leaves = leaves;
        }

        @Override
        public Optional<V> get(int shift, int h, K k) {
            if (this.hash != h) {
                return Optional.empty();
            }
            for (LeafNode<K, V> leaf : leaves) {
                if (Objects.equals(leaf.key, k)) {
                    return Optional.of(leaf.value);
                }
            }
            return Optional.empty();
        }

        @Override
        public Node<K, V> put(int shift, int h, K k, V v, boolean[] added) {
            if (this.hash == h) {
                for (int i = 0; i < leaves.length; i++) {
                    if (Objects.equals(leaves[i].key, k)) {
                        if (Objects.equals(leaves[i].value, v)) {
                            return this;
                        }
                        LeafNode<K, V>[] newLeaves = leaves.clone();
                        newLeaves[i] = new LeafNode<>(h, k, v);
                        return new CollisionNode<>(h, newLeaves);
                    }
                }
                LeafNode<K, V>[] newLeaves = Arrays.copyOf(leaves, leaves.length + 1);
                newLeaves[leaves.length] = new LeafNode<>(h, k, v);
                added[0] = true;
                return new CollisionNode<>(h, newLeaves);
            }
            added[0] = true;
            return mergeLeaves(shift, this, new LeafNode<>(h, k, v));
        }

        @Override
        @SuppressWarnings("unchecked")
        public Node<K, V> remove(int shift, int h, K k, boolean[] removed) {
            if (this.hash != h) {
                return this;
            }

            for (int i = 0; i < leaves.length; i++) {
                if (Objects.equals(leaves[i].key, k)) {
                    removed[0] = true;
                    if (leaves.length == 2) {
                        return i == 0 ? leaves[1] : leaves[0];
                    }
                    LeafNode<K, V>[] newLeaves = new LeafNode[leaves.length - 1];
                    System.arraycopy(leaves, 0, newLeaves, 0, i);
                    System.arraycopy(leaves, i + 1, newLeaves, i, leaves.length - i - 1);
                    return new CollisionNode<>(h, newLeaves);
                }
            }
            return this;
        }
    }

    /**
     * The core routing node. Uses an integer bitmap to compress a 32-wide array
     * into an array only as large as its populated elements.
     */
    private static final class BitmapIndexedNode<K, V> implements Node<K, V> {

        final int bitmap;
        final Node<K, V>[] nodes;

        BitmapIndexedNode(int bitmap, Node<K, V>[] nodes) {
            this.bitmap = bitmap;
            this.nodes = nodes;
        }

        @Override
        public Optional<V> get(int shift, int hash, K key) {
            int bit = bitpos(hash, shift);
            if ((bitmap & bit) == 0) {
                return Optional.empty(); // Not found
            }
            // POPCOUNT: Count set bits to the right of our target bit to find the dense array index.
            int idx = Integer.bitCount(bitmap & (bit - 1));
            return nodes[idx].get(shift + BITS_PER_LEVEL, hash, key);
        }

        @Override
        public Node<K, V> put(int shift, int hash, K key, V value, boolean[] added) {
            int bit = bitpos(hash, shift);
            int idx = Integer.bitCount(bitmap & (bit - 1));

            if ((bitmap & bit) != 0) { // Slot exists, recurse down
                Node<K, V> child = nodes[idx];
                Node<K, V> newChild = child.put(shift + BITS_PER_LEVEL, hash, key, value, added);
                if (newChild == child) {
                    return this;
                }

                Node<K, V>[] newNodes = nodes.clone();
                newNodes[idx] = newChild;
                return new BitmapIndexedNode<>(bitmap, newNodes);
            } else { // Slot is empty, expand the array to fit the new leaf
                Node<K, V>[] newNodes = Arrays.copyOf(nodes, nodes.length + 1);
                System.arraycopy(nodes, idx, newNodes, idx + 1, nodes.length - idx);
                newNodes[idx] = new LeafNode<>(hash, key, value);
                added[0] = true;
                return new BitmapIndexedNode<>(bitmap | bit, newNodes);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Node<K, V> remove(int shift, int hash, K key, boolean[] removed) {
            int bit = bitpos(hash, shift);
            if ((bitmap & bit) == 0) {
                return this;
            }

            int idx = Integer.bitCount(bitmap & (bit - 1));
            Node<K, V> child = nodes[idx];
            Node<K, V> newChild = child.remove(shift + BITS_PER_LEVEL, hash, key, removed);

            if (newChild == child) {
                return this;
            }

            if (newChild instanceof EmptyNode) {
                if (bitmap == bit) {
                    return emptyNode(); // Node is now completely empty
                }
                // If only one element remains and it's a leaf, collapse this node
                if (nodes.length == 2 && nodes[idx ^ 1] instanceof LeafNode) {
                    return nodes[idx ^ 1];
                }

                // Shrink the array
                Node<K, V>[] newNodes = new Node[nodes.length - 1];
                System.arraycopy(nodes, 0, newNodes, 0, idx);
                System.arraycopy(nodes, idx + 1, newNodes, idx, nodes.length - idx - 1);
                return new BitmapIndexedNode<>(bitmap ^ bit, newNodes);
            }

            Node<K, V>[] newNodes = nodes.clone();
            newNodes[idx] = newChild;
            return new BitmapIndexedNode<>(bitmap, newNodes);
        }
    }

    private static class SerializationProxy<K, V> implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private final Object[] keys;
        private final Object[] values;

        SerializationProxy(PersistentHashMap<K, V> map) {
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
            PersistentHashMap<K, V> map = PersistentHashMap.empty();
            for (int i = 0; i < keys.length; i++) {
                map = map.put((K) keys[i], (V) values[i]);
            }
            return map;
        }
    }
}
