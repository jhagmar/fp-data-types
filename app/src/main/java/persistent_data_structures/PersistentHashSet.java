package persistent_data_structures;

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * An immutable, persistent hash set based on a Hash Array Mapped Trie (HAMT).
 * Lookups, insertions, and deletions are effectively O(1) time (technically
 * O(log32 N)). This collection does not permit {@code null} elements.
 * <p>
 * A HAMT uses the 32-bit hash code of an element to navigate a trie. At each level,
 * it consumes 5 bits of the hash to find a path (yielding 2^5 = 32 possible
 * branches). To save memory, sparse arrays are compressed using a bitmap (an
 * {@code int}). A set bit indicates a child exists at that index, and
 * {@code Integer.bitCount()} maps the sparse 32-wide index into a dense array
 * index.
 * <p>
 * When modified, the set does not change in place. Instead, it copies the nodes
 * along the path to the mutated element (Path Copying), sharing the rest of the
 * untouched tree with the older versions of the set.
 *
 * @param <E> the type of elements maintained by this set
 */
public final class PersistentHashSet<E> implements Iterable<E>, Serializable {

    // Singleton instance for the empty set to avoid unnecessary object creation.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static final PersistentHashSet EMPTY = new PersistentHashSet<>(0, EmptyNode.INSTANCE);

    private static final int BITS_PER_LEVEL = 5;

    // 32 possible branches per node
    private static final int BRANCHING_FACTOR = 1 << BITS_PER_LEVEL;

    // Mask to extract 5 bits (binary 11111, decimal 31)
    private static final int LEVEL_MASK = BRANCHING_FACTOR - 1;

    // Transient fields: Java's default serialization is bypassed via writeReplace()
    private final transient int size;
    private final transient Node<E> root;

    private PersistentHashSet(int size, Node<E> root) {
        this.size = size;
        this.root = root;
    }

    /**
     * Returns an empty PersistentHashSet.
     *
     * @return Empty PersistentHashSet instance
     */
    @SuppressWarnings("unchecked")
    public static <E> PersistentHashSet<E> empty() {
        return (PersistentHashSet<E>) EMPTY;
    }

    /**
     * Re-hashes the standard Java hashCode using the MurmurHash3 32-bit
     * finalizer. This guarantees a good avalanche effect, ensuring a uniform
     * distribution across the HAMT branches.
     */
    private static int hash(Object element) {
        int h = element.hashCode();

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
    private static <E> Node<E> emptyNode() {
        return (Node<E>) EmptyNode.INSTANCE;
    }

    @SuppressWarnings("unchecked")
    private static <E> Node<E> mergeLeaves(int shift, Node<E> n1, Node<E> n2) {
        int h1 = getHash(n1);
        int h2 = getHash(n2);

        if (h1 == h2) {
            return new CollisionNode<>(h1, new LeafNode[]{(LeafNode<E>) n1, (LeafNode<E>) n2});
        }

        int bit1 = bitpos(h1, shift);
        int bit2 = bitpos(h2, shift);

        if (bit1 == bit2) {
            Node<E> child = mergeLeaves(shift + BITS_PER_LEVEL, n1, n2);
            return new BitmapIndexedNode<>(bit1, new Node[]{child});
        } else {
            // Use unsigned comparison so the 31st bit (0x80000000) is treated as the maximum value
            return new BitmapIndexedNode<>(bit1 | bit2,
                    Integer.compareUnsigned(bit1, bit2) < 0 ? new Node[]{n1, n2} : new Node[]{n2, n1});
        }
    }

    private static int getHash(Node<?> node) {
        Objects.requireNonNull(node, "Node cannot be null when calculating hash");
        if (node instanceof LeafNode) {
            return ((LeafNode<?>) node).hash;
        }
        if (node instanceof CollisionNode) {
            return ((CollisionNode<?>) node).hash;
        }
        throw new IllegalStateException("Unexpected node type in merge: " + node.getClass().getSimpleName());
    }

    /**
     * Checks if the set is empty.
     *
     * @return {@code true} if the set is empty, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns the number of elements in the set.
     *
     * @return the number of elements in the set
     */
    public int size() {
        return size;
    }

    /**
     * Checks if the set contains the specified element.
     *
     * @param element the element whose presence in the set is to be tested
     * @return {@code true} if the set contains the element, {@code false} otherwise
     * @throws NullPointerException if the specified element is null
     */
    public boolean contains(E element) {
        Objects.requireNonNull(element, "PersistentHashSet does not permit null elements");
        return root.contains(0, hash(element), element);
    }

    /**
     * Adds the specified element to the set if it is not already present.
     *
     * @param element the element to be added to the set
     * @return a new PersistentHashSet instance with the specified element added,
     * or the same instance if the element was already present
     * @throws NullPointerException if the specified element is null
     */
    public PersistentHashSet<E> add(E element) {
        Objects.requireNonNull(element, "PersistentHashSet does not permit null elements");

        Node<E> newRoot = root.add(0, hash(element), element);

        if (newRoot == root) {
            return this; // No changes made (element already existed)
        }
        return new PersistentHashSet<>(size + 1, newRoot);
    }

    /**
     * Removes the specified element from the set if it is present.
     *
     * @param element the element to be removed from the set
     * @return a new PersistentHashSet instance with the specified element removed,
     * or the same instance if the element was not found
     * @throws NullPointerException if the specified element is null
     */
    public PersistentHashSet<E> remove(E element) {
        Objects.requireNonNull(element, "PersistentHashSet does not permit null elements");

        boolean[] removed = new boolean[1];
        Node<E> newRoot = root.remove(0, hash(element), element, removed);

        if (!removed[0]) {
            return this; // Element was not found
        }
        return new PersistentHashSet<>(size - 1, newRoot);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            // Max depth of a 32-bit hash processed 5 bits at a time is ceil(32/5) = 7.
            // Using a fixed size array of 8 bypasses the overhead of java.util.Stack.
            @SuppressWarnings("unchecked")
            private final Node<E>[] nodePath = new Node[8];
            private final int[] indexPath = new int[8];
            private int depth = -1;

            private LeafNode<E> nextLeaf;

            // State for fully iterating through Hash Collisions
            private CollisionNode<E> currentCollision;
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
                    Node<E> node = nodePath[depth];
                    int idx = indexPath[depth];

                    switch (node) {
                        case LeafNode<E> leafNode -> {
                            nextLeaf = leafNode;
                            depth--; // Step back up

                            return; // Step back up
                        }
                        case CollisionNode<E> collisionNode -> {
                            currentCollision = collisionNode;
                            collisionIdx = 1; // Prepare the iterator to pick up at index 1 next time

                            nextLeaf = currentCollision.leaves[0];
                            depth--;
                            return;
                        }
                        case BitmapIndexedNode<E> bin -> {
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
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                E element = nextLeaf.element;
                advance();
                return element;
            }
        };
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PersistentHashSet)) {
            return false;
        }

        @SuppressWarnings("unchecked")
        PersistentHashSet<E> that = (PersistentHashSet<E>) o;
        if (this.size != that.size) {
            return false;
        }

        for (E element : this) {
            if (!that.contains(element)) {
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
        return sb.append("]").toString();
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
    private interface Node<E> {

        boolean contains(int shift, int hash, E element);

        Node<E> add(int shift, int hash, E element);

        Node<E> remove(int shift, int hash, E element, boolean[] removed);
    }

    private static final class EmptyNode<E> implements Node<E> {

        @SuppressWarnings("rawtypes")
        static final EmptyNode INSTANCE = new EmptyNode();

        @Override
        public boolean contains(int shift, int hash, E element) {
            return false;
        }

        @Override
        public Node<E> add(int shift, int hash, E element) {
            return new LeafNode<>(hash, element);
        }

        @Override
        public Node<E> remove(int shift, int hash, E element, boolean[] removed) {
            return this;
        }
    }

    private static final class LeafNode<E> implements Node<E> {

        final int hash;
        final E element;

        LeafNode(int hash, E element) {
            this.hash = hash;
            this.element = element;
        }

        @Override
        public boolean contains(int shift, int h, E e) {
            return this.hash == h && Objects.equals(this.element, e);
        }

        @Override
        public Node<E> add(int shift, int h, E e) {
            if (this.hash == h && Objects.equals(this.element, e)) {
                return this; // Element already exists
            }
            // Element is different; merge the existing leaf and the new leaf into a branching node
            return mergeLeaves(shift, this, new LeafNode<>(h, e));
        }

        @Override
        public Node<E> remove(int shift, int h, E e, boolean[] removed) {
            if (this.hash == h && Objects.equals(this.element, e)) {
                removed[0] = true;
                return emptyNode();
            }
            return this;
        }
    }

    /**
     * Handles the rare case where two distinct elements evaluate to the exact same
     * 32-bit hash. Instead of branching, they are stored linearly in an array.
     */
    private static final class CollisionNode<E> implements Node<E> {

        final int hash;
        final LeafNode<E>[] leaves;

        CollisionNode(int hash, LeafNode<E>[] leaves) {
            this.hash = hash;
            this.leaves = leaves;
        }

        @Override
        public boolean contains(int shift, int h, E e) {
            if (this.hash != h) {
                return false;
            }
            for (LeafNode<E> leaf : leaves) {
                if (Objects.equals(leaf.element, e)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Node<E> add(int shift, int h, E e) {
            if (this.hash == h) {
                for (LeafNode<E> leaf : leaves) {
                    if (Objects.equals(leaf.element, e)) {
                        return this; // Already present in collision list
                    }
                }
                LeafNode<E>[] newLeaves = Arrays.copyOf(leaves, leaves.length + 1);
                newLeaves[leaves.length] = new LeafNode<>(h, e);
                return new CollisionNode<>(h, newLeaves);
            }
            return mergeLeaves(shift, this, new LeafNode<>(h, e));
        }

        @Override
        @SuppressWarnings("unchecked")
        public Node<E> remove(int shift, int h, E e, boolean[] removed) {
            if (this.hash != h) {
                return this;
            }

            for (int i = 0; i < leaves.length; i++) {
                if (Objects.equals(leaves[i].element, e)) {
                    removed[0] = true;
                    if (leaves.length == 2) {
                        return i == 0 ? leaves[1] : leaves[0];
                    }
                    LeafNode<E>[] newLeaves = new LeafNode[leaves.length - 1];
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
    private static final class BitmapIndexedNode<E> implements Node<E> {

        final int bitmap;
        final Node<E>[] nodes;

        BitmapIndexedNode(int bitmap, Node<E>[] nodes) {
            this.bitmap = bitmap;
            this.nodes = nodes;
        }

        @Override
        public boolean contains(int shift, int hash, E element) {
            int bit = bitpos(hash, shift);
            if ((bitmap & bit) == 0) {
                return false; // Not found
            }
            // POPCOUNT: Count set bits to the right of our target bit to find the dense array index.
            int idx = Integer.bitCount(bitmap & (bit - 1));
            return nodes[idx].contains(shift + BITS_PER_LEVEL, hash, element);
        }

        @Override
        public Node<E> add(int shift, int hash, E element) {
            int bit = bitpos(hash, shift);
            int idx = Integer.bitCount(bitmap & (bit - 1));

            if ((bitmap & bit) != 0) { // Slot exists, recurse down
                Node<E> child = nodes[idx];
                Node<E> newChild = child.add(shift + BITS_PER_LEVEL, hash, element);
                if (newChild == child) {
                    return this;
                }

                Node<E>[] newNodes = nodes.clone();
                newNodes[idx] = newChild;
                return new BitmapIndexedNode<>(bitmap, newNodes);
            } else { // Slot is empty, expand the array to fit the new leaf
                Node<E>[] newNodes = Arrays.copyOf(nodes, nodes.length + 1);
                System.arraycopy(nodes, idx, newNodes, idx + 1, nodes.length - idx);
                newNodes[idx] = new LeafNode<>(hash, element);
                return new BitmapIndexedNode<>(bitmap | bit, newNodes);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Node<E> remove(int shift, int hash, E element, boolean[] removed) {
            int bit = bitpos(hash, shift);
            if ((bitmap & bit) == 0) {
                return this;
            }

            int idx = Integer.bitCount(bitmap & (bit - 1));
            Node<E> child = nodes[idx];
            Node<E> newChild = child.remove(shift + BITS_PER_LEVEL, hash, element, removed);

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
                Node<E>[] newNodes = new Node[nodes.length - 1];
                System.arraycopy(nodes, 0, newNodes, 0, idx);
                System.arraycopy(nodes, idx + 1, newNodes, idx, nodes.length - idx - 1);
                return new BitmapIndexedNode<>(bitmap ^ bit, newNodes);
            }

            Node<E>[] newNodes = nodes.clone();
            newNodes[idx] = newChild;
            return new BitmapIndexedNode<>(bitmap, newNodes);
        }
    }

    private static class SerializationProxy<E> implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
        private final Object[] elements;

        SerializationProxy(PersistentHashSet<E> set) {
            this.elements = new Object[set.size()];
            int i = 0;
            for (E element : set) {
                this.elements[i++] = element;
            }
        }

        @Serial
        @SuppressWarnings("unchecked")
        private Object readResolve() {
            PersistentHashSet<E> set = PersistentHashSet.empty();
            for (Object element : elements) {
                set = set.add((E) element);
            }
            return set;
        }
    }
}