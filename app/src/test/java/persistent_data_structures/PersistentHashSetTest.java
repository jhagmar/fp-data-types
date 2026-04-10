package persistent_data_structures;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class PersistentHashSetTest {

    @Test
    void testEmptySet() {
        PersistentHashSet<String> set = PersistentHashSet.empty();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains("A"));
    }

    @Test
    void testAddAndContains() {
        PersistentHashSet<String> set = PersistentHashSet.<String>empty()
                .add("One")
                .add("Two")
                .add("Three");

        assertFalse(set.isEmpty());
        assertEquals(3, set.size());
        assertTrue(set.contains("One"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("Three"));
        assertFalse(set.contains("Four"));
    }

    @Test
    void testAddImmutability() {
        PersistentHashSet<String> set1 = PersistentHashSet.empty();
        PersistentHashSet<String> set2 = set1.add("A");
        PersistentHashSet<String> set3 = set2.add("B");

        assertTrue(set1.isEmpty());
        assertEquals(1, set2.size());
        assertEquals(2, set3.size());

        assertFalse(set1.contains("A"));
        assertTrue(set2.contains("A"));
        assertFalse(set2.contains("B"));
        assertTrue(set3.contains("A"));
        assertTrue(set3.contains("B"));
    }

    @Test
    void testAddOptimizationSameValueReturnsSelf() {
        PersistentHashSet<String> set1 = PersistentHashSet.<String>empty().add("A");
        PersistentHashSet<String> set2 = set1.add("A");

        assertSame(set1, set2, "Adding an already present element should return the identical instance");
    }

    @Test
    void testNullElementsRejected() {
        PersistentHashSet<String> set = PersistentHashSet.empty();

        NullPointerException exception1 = assertThrows(NullPointerException.class, () -> set.add(null));
        assertEquals("PersistentHashSet does not permit null elements", exception1.getMessage());

        NullPointerException exception2 = assertThrows(NullPointerException.class, () -> set.contains(null));
        assertEquals("PersistentHashSet does not permit null elements", exception2.getMessage());

        NullPointerException exception3 = assertThrows(NullPointerException.class, () -> set.remove(null));
        assertEquals("PersistentHashSet does not permit null elements", exception3.getMessage());
    }

    @Test
    void testRemoveNonExistentElementReturnsSelf() {
        PersistentHashSet<String> set = PersistentHashSet.<String>empty().add("A").add("B");
        PersistentHashSet<String> result = set.remove("C");
        assertSame(set, result, "Removing a non-existent element should return the same instance without modification");
    }

    @Test
    void testRemoveEmptyNode() {
        PersistentHashSet<String> set = PersistentHashSet.<String>empty();
        PersistentHashSet<String> result = set.remove("A");

        assertSame(set, result, "Removing from an empty set should return the same instance");
        assertEquals(0, result.size());
    }

    @Test
    void testRemoveLeafNode() {
        PersistentHashSet<String> set = PersistentHashSet.<String>empty()
                .add("A").add("B").add("C");
        PersistentHashSet<String> result = set.remove("B");

        assertEquals(2, result.size());
        assertFalse(result.contains("B"));
        assertTrue(result.contains("A"));
        assertTrue(result.contains("C"));
    }

    @Test
    void testLeafNodeBranchCoverage() {
        // Create two distinct elements that share the exact same hash code
        CollidingElement e1 = new CollidingElement(1);
        CollidingElement e2 = new CollidingElement(2);

        // Create an element with a different hash code
        CollidingElement eDifferentHash = new CollidingElement(99) {
            @Override
            public int hashCode() {
                return 999;
            }
        };

        // Insert exactly ONE item. 
        // This ensures the root of the set is a LeafNode, not a BitmapIndexedNode or CollisionNode.
        PersistentHashSet<CollidingElement> set = PersistentHashSet.<CollidingElement>empty().add(e1);

        // Cover LeafNode.contains() branches
        // Branch 1: hash == h (TRUE) && equals(element) (TRUE) -> Hit
        assertTrue(set.contains(e1));

        // Branch 2: hash == h (TRUE) && equals(element) (FALSE) -> Miss
        assertFalse(set.contains(e2));

        // Branch 3: hash == h (FALSE) -> Miss
        assertFalse(set.contains(eDifferentHash));

        // Cover LeafNode.remove() branches
        // Branch 2: hash == h (TRUE) && equals(element) (FALSE) -> No change
        PersistentHashSet<CollidingElement> setAfterE2Remove = set.remove(e2);
        assertSame(set, setAfterE2Remove);

        // Branch 3: hash == h (FALSE) -> No change
        PersistentHashSet<CollidingElement> setAfterDiffHashRemove = set.remove(eDifferentHash);
        assertSame(set, setAfterDiffHashRemove);

        // Branch 1: hash == h (TRUE) && equals(element) (TRUE) -> Removed successfully
        PersistentHashSet<CollidingElement> setAfterE1Remove = set.remove(e1);
        assertTrue(setAfterE1Remove.isEmpty());
    }

    @Test
    void testDeepTrieExpansionAndCompaction() {
        // Insert enough elements to guarantee deep branching (BitmapIndexedNodes over multiple depths)
        PersistentHashSet<Integer> set = PersistentHashSet.empty();
        for (int i = 0; i < 1000; i++) {
            set = set.add(i);
        }
        assertEquals(1000, set.size());

        for (int i = 0; i < 1000; i++) {
            assertTrue(set.contains(i));
        }

        // Remove all elements to trigger array compaction and EmptyNode edge cases
        for (int i = 0; i < 1000; i++) {
            set = set.remove(i);
        }
        assertTrue(set.isEmpty());
    }

    // --- Hash Collision Tests ---

    @Test
    void testHashCollisionsAddAndContains() {
        CollidingElement e1 = new CollidingElement(1);
        CollidingElement e2 = new CollidingElement(2);
        CollidingElement e3 = new CollidingElement(3);

        PersistentHashSet<CollidingElement> set = PersistentHashSet.<CollidingElement>empty()
                .add(e1)
                .add(e2)
                .add(e3);

        assertEquals(3, set.size());
        assertTrue(set.contains(e1));
        assertTrue(set.contains(e2));
        assertTrue(set.contains(e3));
        assertFalse(set.contains(new CollidingElement(4)));
    }

    @Test
    void testHashCollisionsUpdateIdenticalYieldsSelf() {
        CollidingElement e1 = new CollidingElement(1);
        CollidingElement e2 = new CollidingElement(2);

        PersistentHashSet<CollidingElement> set1 = PersistentHashSet.<CollidingElement>empty()
                .add(e1).add(e2);

        PersistentHashSet<CollidingElement> set2 = set1.add(e1);
        assertSame(set1, set2);
    }

    @Test
    void testHashCollisionsRemove() {
        CollidingElement e1 = new CollidingElement(1);
        CollidingElement e2 = new CollidingElement(2);
        CollidingElement e3 = new CollidingElement(3);
        CollidingElement e4 = new CollidingElement(4);

        PersistentHashSet<CollidingElement> set = PersistentHashSet.<CollidingElement>empty()
                .add(e1).add(e2).add(e3).add(e4);

        // Remove from >2 length array
        set = set.remove(e2);
        assertEquals(3, set.size());
        assertFalse(set.contains(e2));

        // Remove from a 3 length array down to 2
        set = set.remove(e1);
        assertEquals(2, set.size());

        // Remove from a 2 length array down to 1 (should unwrap from CollisionNode)
        set = set.remove(e3);
        assertEquals(1, set.size());
        assertTrue(set.contains(e4));

        // Remove last
        set = set.remove(e4);
        assertTrue(set.isEmpty());
    }

    @Test
    void testCollisionNodeBranchCoverage() {
        CollidingElement e1 = new CollidingElement(1);
        CollidingElement e2 = new CollidingElement(2);
        CollidingElement e3NotInSet = new CollidingElement(3); // Same hash, not inserted

        CollidingElement eDifferentHash = new CollidingElement(99) {
            @Override
            public int hashCode() {
                return 999;
            }
        };

        // Create a set with a CollisionNode at the root.
        PersistentHashSet<CollidingElement> set = PersistentHashSet.<CollidingElement>empty()
                .add(e1).add(e2);

        // --- Cover CollisionNode.contains() missing branches ---
        // Branch: hash == h (TRUE), but loop finishes without matching element
        assertFalse(set.contains(e3NotInSet));

        // Branch: hash != h (FALSE)
        assertFalse(set.contains(eDifferentHash));

        // --- Cover CollisionNode.add() missing branches ---
        // Branch: add an element with a DIFFERENT hash into an existing CollisionNode.
        PersistentHashSet<CollidingElement> setAfterDiffAdd = set.add(eDifferentHash);
        assertEquals(3, setAfterDiffAdd.size());
        assertTrue(setAfterDiffAdd.contains(eDifferentHash));
        assertTrue(setAfterDiffAdd.contains(e1));
        assertTrue(setAfterDiffAdd.contains(e2));

        // --- Cover CollisionNode.remove() missing branches ---
        // Branch: hash == h (TRUE), but loop finishes without matching element
        PersistentHashSet<CollidingElement> setAfterE3Remove = set.remove(e3NotInSet);
        assertSame(set, setAfterE3Remove, "Removing a non-existent element with the same hash should return self");

        // Branch: hash != h (FALSE)
        PersistentHashSet<CollidingElement> setAfterDiffRemove = set.remove(eDifferentHash);
        assertSame(set, setAfterDiffRemove, "Removing an element with a different hash should return self");

        // --- Cover CollisionNode Collapse Ternary (i == 0 ? leaves[1] : leaves[0]) ---
        // Ternary True (i == 0): Remove e1 (which is at index 0). Should return e2.
        PersistentHashSet<CollidingElement> setAfterE1Remove = set.remove(e1);
        assertEquals(1, setAfterE1Remove.size());
        assertTrue(setAfterE1Remove.contains(e2));
        assertFalse(setAfterE1Remove.contains(e1));

        // Ternary False (i == 1): Remove e2 (which is at index 1). Should return e1.
        PersistentHashSet<CollidingElement> setAfterE2Remove = set.remove(e2);
        assertEquals(1, setAfterE2Remove.size());
        assertTrue(setAfterE2Remove.contains(e1));
        assertFalse(setAfterE2Remove.contains(e2));
    }

    @Test
    void testGetHashThrowsOnInvalidNode() throws Exception {
        // 1. Resolve the private Node interface
        Class<?> nodeInterface = Class.forName("persistent_data_structures.PersistentHashSet$Node");

        // 2. Resolve the private getHash method and make it accessible
        Method getHashMethod = PersistentHashSet.class.getDeclaredMethod("getHash", nodeInterface);
        getHashMethod.setAccessible(true);

        // 3. Resolve the private EmptyNode class and get its INSTANCE field
        Class<?> emptyNodeClass = Class.forName("persistent_data_structures.PersistentHashSet$EmptyNode");
        java.lang.reflect.Field instanceField = emptyNodeClass.getDeclaredField("INSTANCE");
        instanceField.setAccessible(true);
        Object emptyNodeInstance = instanceField.get(null);

        // 4. Invoke the method with the invalid node type
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> getHashMethod.invoke(null, emptyNodeInstance)
        );

        // 5. Assert the correct exception was thrown
        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertEquals("Unexpected node type in merge: EmptyNode", exception.getCause().getMessage());
    }

    @Test
    void testEmptySetIteratorCoverage() {
        PersistentHashSet<String> set = PersistentHashSet.empty();
        Iterator<String> it = set.iterator();

        assertFalse(it.hasNext(), "Empty set iterator should not have next");
        NoSuchElementException nsee = assertThrows(NoSuchElementException.class, it::next, "Calling next() on empty set iterator throws");
        assertNotNull(nsee);
    }

    @Test
    void testBitmapIndexedNodeReturnsSelf() {
        CollidingElement e1 = new CollidingElement(1); // Hash is 42
        CollidingElement e2 = new CollidingElement(2); // Hash is 42 (Collides with e1)

        CollidingElement eDifferent = new CollidingElement(99) {
            @Override
            public int hashCode() {
                return 999;
            } // Different hash
        };

        // 1. Create a set with a BitmapIndexedNode at the root.
        PersistentHashSet<CollidingElement> set = PersistentHashSet.<CollidingElement>empty()
                .add(e1)
                .add(eDifferent);

        // --- Cover BitmapIndexedNode.add() -> if (newChild == child) return this; ---
        PersistentHashSet<CollidingElement> setAfterSameAdd = set.add(e1);
        assertSame(set, setAfterSameAdd, "Adding an identical element through a BitmapIndexedNode should return self");

        // --- Cover BitmapIndexedNode.remove() -> if (newChild == child) return this; ---
        PersistentHashSet<CollidingElement> setAfterMissingRemove = set.remove(e2);
        assertSame(set, setAfterMissingRemove, "Removing a missing element that routes through an existing BitmapIndexedNode branch should return self");
    }

    @Test
    void testIteratorThrowsOnInvalidNode() throws Exception {
        // 1. Resolve the private Node interface
        Class<?> nodeInterface = Class.forName("persistent_data_structures.PersistentHashSet$Node");

        // 2. Create a dynamic proxy to act as a rogue Node implementation
        Object bogusNode = java.lang.reflect.Proxy.newProxyInstance(
                PersistentHashSet.class.getClassLoader(),
                new Class<?>[]{nodeInterface},
                (proxy, method, args) -> null
        );

        // 3. Create a new set instance with the bogus node via reflection.
        java.lang.reflect.Constructor<?> constructor = PersistentHashSet.class.getDeclaredConstructor(int.class, nodeInterface);
        constructor.setAccessible(true);

        @SuppressWarnings("unchecked")
        PersistentHashSet<String> bogusSet = (PersistentHashSet<String>) constructor.newInstance(1, bogusNode);

        // 4. Calling iterator() triggers the instance initializer, which calls advance() and hits the exception
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                bogusSet::iterator
        );

        assertTrue(exception.getMessage().contains("Unknown node type in iterator"));
    }

    // --- Iterator & Utility Tests ---
    @Test
    void testIteratorExhaustion() {
        PersistentHashSet<String> set = PersistentHashSet.<String>empty()
                .add("A").add("B");

        Iterator<String> it = set.iterator();

        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertFalse(it.hasNext());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, it::next);
        assertNotNull(exception);
    }

    @Test
    void testIteratorWithCollisions() {
        CollidingElement e1 = new CollidingElement(1);
        CollidingElement e2 = new CollidingElement(2);

        PersistentHashSet<CollidingElement> set = PersistentHashSet.<CollidingElement>empty()
                .add(e1).add(e2);

        int count = 0;
        for (@SuppressWarnings("unused") CollidingElement ignored : set) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void testEqualsAndHashCode() {
        PersistentHashSet<String> set1 = PersistentHashSet.<String>empty().add("A").add("B").add("C");
        PersistentHashSet<String> set2 = PersistentHashSet.<String>empty().add("C").add("A").add("B");
        PersistentHashSet<String> set3 = PersistentHashSet.<String>empty().add("A").add("C");
        PersistentHashSet<String> set4 = PersistentHashSet.<String>empty().add("A").add("Z").add("C");

        assertEquals(set1, set1); // self
        assertEquals(set1, set2); // same elements, different insertion order
        assertNotEquals(set1, set3); // different size
        assertNotEquals(set1, set4); // same size, different elements
        assertNotEquals(set1, new Object()); // different type
        assertNotEquals(set1, null);

        assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Test
    void testToString() {
        PersistentHashSet<String> emptySet = PersistentHashSet.empty();
        assertEquals("[]", emptySet.toString());

        PersistentHashSet<String> set1 = PersistentHashSet.<String>empty().add("A");
        assertEquals("[A]", set1.toString());

        PersistentHashSet<String> set2 = set1.add("B");
        String str = set2.toString();
        // HAMT doesn't guarantee order, so it could be [A, B] or [B, A]
        assertTrue(str.equals("[A, B]") || str.equals("[B, A]"));
    }

    // --- Serialization Proxy Tests ---
    @Test
    void testSerializationRoundTrip() throws Exception {
        PersistentHashSet<String> original = PersistentHashSet.<String>empty()
                .add("A")
                .add("B")
                .add("C");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PersistentHashSet<String> deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            @SuppressWarnings("unchecked")
            PersistentHashSet<String> casted = (PersistentHashSet<String>) ois.readObject();
            deserialized = casted;
        }

        assertNotSame(original, deserialized);
        assertEquals(original, deserialized);
        assertEquals(original.size(), deserialized.size());
    }

    @Test
    void testDirectReadObjectThrowsException() throws Exception {
        PersistentHashSet<String> set = PersistentHashSet.empty();

        // Use reflection to bypass visibility and invoke readObject directly to guarantee 100% line coverage
        Method readObjectMethod = PersistentHashSet.class.getDeclaredMethod("readObject", ObjectInputStream.class);
        readObjectMethod.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> readObjectMethod.invoke(set, (ObjectInputStream) null)
        );

        assertTrue(exception.getCause() instanceof java.io.InvalidObjectException);
        assertEquals("Serialization proxy required", exception.getCause().getMessage());
    }

    /**
     * A helper class that deliberately forces hash collisions to ensure full
     * coverage of the CollisionNode logic.
     */
    static class CollidingElement {

        final int id;

        CollidingElement(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return 42; // Force identical hash codes
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CollidingElement)) {
                return false;
            }
            return id == ((CollidingElement) o).id;
        }

        @Override
        public String toString() {
            return "E" + id;
        }
    }
}