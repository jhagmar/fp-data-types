package persistent_data_structures;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PersistentHashMapTest {

    @Test
    void testEmptyMap() {
        PersistentHashMap<Integer, String> map = PersistentHashMap.empty();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(Optional.empty(), map.get(1));
        assertFalse(map.containsKey(1));
    }

    @Test
    void testPutAndGet() {
        PersistentHashMap<Integer, String> map = PersistentHashMap.<Integer, String>empty()
                .put(1, "One")
                .put(2, "Two")
                .put(3, "Three");

        assertFalse(map.isEmpty());
        assertEquals(3, map.size());
        assertEquals(Optional.of("One"), map.get(1));
        assertEquals(Optional.of("Two"), map.get(2));
        assertEquals(Optional.of("Three"), map.get(3));
        assertEquals(Optional.empty(), map.get(4));

        assertTrue(map.containsKey(2));
        assertFalse(map.containsKey(4));
    }

    @Test
    void testPutImmutability() {
        PersistentHashMap<Integer, String> map1 = PersistentHashMap.empty();
        PersistentHashMap<Integer, String> map2 = map1.put(1, "A");
        PersistentHashMap<Integer, String> map3 = map2.put(2, "B");

        assertTrue(map1.isEmpty());
        assertEquals(1, map2.size());
        assertEquals(2, map3.size());

        assertFalse(map1.containsKey(1));
        assertTrue(map2.containsKey(1));
        assertTrue(map3.containsKey(2));
    }

    @Test
    void testPutUpdatesExistingValue() {
        PersistentHashMap<Integer, String> map1 = PersistentHashMap.<Integer, String>empty().put(1, "A");
        PersistentHashMap<Integer, String> map2 = map1.put(1, "B");

        assertEquals(1, map1.size());
        assertEquals(1, map2.size());
        assertEquals(Optional.of("A"), map1.get(1));
        assertEquals(Optional.of("B"), map2.get(1));
    }

    @Test
    void testPutOptimizationSameValueReturnsSelf() {
        PersistentHashMap<Integer, String> map1 = PersistentHashMap.<Integer, String>empty().put(1, "A");
        PersistentHashMap<Integer, String> map2 = map1.put(1, "A");

        assertSame(map1, map2, "Putting the exact same key-value pair should return the identical instance");
    }

    @Test
    void testNullKeysRejected() {
        PersistentHashMap<Integer, String> map = PersistentHashMap.empty();
        NullPointerException exception1 = assertThrows(NullPointerException.class, () -> map.put(null, "A"));
        assertEquals("PersistentHashMap does not permit null keys", exception1.getMessage());

        NullPointerException exception2 = assertThrows(NullPointerException.class, () -> map.get(null));
        assertEquals("PersistentHashMap does not permit null keys", exception2.getMessage());

        NullPointerException exception3 = assertThrows(NullPointerException.class, () -> map.containsKey(null));
        assertEquals("PersistentHashMap does not permit null keys", exception3.getMessage());

        NullPointerException exception4 = assertThrows(NullPointerException.class, () -> map.remove(null));
        assertEquals("PersistentHashMap does not permit null keys", exception4.getMessage());
    }

    @Test
    void testNullValuesRejected() {
        PersistentHashMap<Integer, String> map = PersistentHashMap.empty();
        NullPointerException exception = assertThrows(NullPointerException.class, () -> map.put(1, null));
        assertEquals("PersistentHashMap does not permit null values", exception.getMessage());
    }

    @Test
    void testRemoveNonExistentKeyReturnsSelf() {
        PersistentHashMap<Integer, String> map = PersistentHashMap.<Integer, String>empty().put(1, "A").put(2, "B");
        PersistentHashMap<Integer, String> result = map.remove(3);
        assertSame(map, result, "Removing a non-existent key should return the same instance without modification");
    }

    @Test
    void testRemoveEmptyNode() {
        PersistentHashMap<Integer, String> map = PersistentHashMap.<Integer, String>empty();
        PersistentHashMap<Integer, String> result = map.remove(2);

        assertSame(map, result, "Removing from an empty map should return the same instance");
        assertEquals(0, result.size());
    }

    @Test
    void testRemoveLeafNode() {
        PersistentHashMap<Integer, String> map = PersistentHashMap.<Integer, String>empty()
                .put(1, "A").put(2, "B").put(3, "C");
        PersistentHashMap<Integer, String> result = map.remove(2);

        assertEquals(2, result.size());
        assertFalse(result.containsKey(2));
        assertTrue(result.containsKey(1));
        assertTrue(result.containsKey(3));
    }

    @Test
    void testLeafNodeBranchCoverage() {
        // Create two distinct keys that share the exact same hash code
        CollidingKey k1 = new CollidingKey(1);
        CollidingKey k2 = new CollidingKey(2);

        // Create a key with a different hash code
        CollidingKey kDifferentHash = new CollidingKey(99) {
            @Override
            public int hashCode() {
                return 999;
            }
        };

        // Insert exactly ONE item. 
        // This ensures the root of the map is a LeafNode, not a BitmapIndexedNode or CollisionNode.
        PersistentHashMap<CollidingKey, String> map = PersistentHashMap.<CollidingKey, String>empty().put(k1, "Target");

        // Cover LeafNode.get() branches
        // Branch 1: hash == h (TRUE) && equals(key) (TRUE) -> Hit
        assertEquals(Optional.of("Target"), map.get(k1));

        // Branch 2: hash == h (TRUE) && equals(key) (FALSE) -> Miss
        // This triggers the missing branch. The hash matches, so the first half of the && passes, 
        // but the equals() check fails.
        assertEquals(Optional.empty(), map.get(k2));

        // Branch 3: hash == h (FALSE) -> Miss
        // Short-circuits immediately.
        assertEquals(Optional.empty(), map.get(kDifferentHash));

        // Cover LeafNode.remove() branches
        // Branch 2: hash == h (TRUE) && equals(key) (FALSE) -> No change
        PersistentHashMap<CollidingKey, String> mapAfterK2Remove = map.remove(k2);
        assertSame(map, mapAfterK2Remove);

        // Branch 3: hash == h (FALSE) -> No change
        PersistentHashMap<CollidingKey, String> mapAfterDiffHashRemove = map.remove(kDifferentHash);
        assertSame(map, mapAfterDiffHashRemove);

        // Branch 1: hash == h (TRUE) && equals(key) (TRUE) -> Removed successfully
        PersistentHashMap<CollidingKey, String> mapAfterK1Remove = map.remove(k1);
        assertTrue(mapAfterK1Remove.isEmpty());
    }

    @Test
    void testDeepTrieExpansionAndCompaction() {
        // Insert enough elements to guarantee deep branching (BitmapIndexedNodes over multiple depths)
        PersistentHashMap<Integer, Integer> map = PersistentHashMap.empty();
        for (int i = 0; i < 1000; i++) {
            map = map.put(i, i * 10);
        }
        assertEquals(1000, map.size());

        for (int i = 0; i < 1000; i++) {
            assertTrue(map.containsKey(i));
            assertEquals(Optional.of(i * 10), map.get(i));
        }

        // Remove all elements to trigger array compaction and EmptyNode edge cases
        for (int i = 0; i < 1000; i++) {
            map = map.remove(i);
        }
        assertTrue(map.isEmpty());
    }

    // --- Hash Collision Tests ---

    @Test
    void testHashCollisionsPutAndGet() {
        CollidingKey k1 = new CollidingKey(1);
        CollidingKey k2 = new CollidingKey(2);
        CollidingKey k3 = new CollidingKey(3);

        PersistentHashMap<CollidingKey, String> map = PersistentHashMap.<CollidingKey, String>empty()
                .put(k1, "One")
                .put(k2, "Two")
                .put(k3, "Three");

        assertEquals(3, map.size());
        assertEquals(Optional.of("One"), map.get(k1));
        assertEquals(Optional.of("Two"), map.get(k2));
        assertEquals(Optional.of("Three"), map.get(k3));
        assertEquals(Optional.empty(), map.get(new CollidingKey(4)));
    }

    @Test
    void testHashCollisionsUpdateExisting() {
        CollidingKey k1 = new CollidingKey(1);
        CollidingKey k2 = new CollidingKey(2);

        PersistentHashMap<CollidingKey, String> map1 = PersistentHashMap.<CollidingKey, String>empty()
                .put(k1, "One").put(k2, "Two");

        PersistentHashMap<CollidingKey, String> map2 = map1.put(k1, "UpdatedOne");

        assertEquals(2, map2.size());
        assertEquals(Optional.of("UpdatedOne"), map2.get(k1));
        assertEquals(Optional.of("Two"), map2.get(k2));
    }

    @Test
    void testHashCollisionsUpdateIdenticalYieldsSelf() {
        CollidingKey k1 = new CollidingKey(1);
        CollidingKey k2 = new CollidingKey(2);

        PersistentHashMap<CollidingKey, String> map1 = PersistentHashMap.<CollidingKey, String>empty()
                .put(k1, "One").put(k2, "Two");

        PersistentHashMap<CollidingKey, String> map2 = map1.put(k1, "One");
        assertSame(map1, map2);
    }

    @Test
    void testHashCollisionsRemove() {
        CollidingKey k1 = new CollidingKey(1);
        CollidingKey k2 = new CollidingKey(2);
        CollidingKey k3 = new CollidingKey(3);
        CollidingKey k4 = new CollidingKey(4);

        PersistentHashMap<CollidingKey, String> map = PersistentHashMap.<CollidingKey, String>empty()
                .put(k1, "A").put(k2, "B").put(k3, "C").put(k4, "D");

        // Remove from >2 length array
        map = map.remove(k2);
        assertEquals(3, map.size());
        assertFalse(map.containsKey(k2));

        // Remove from a 3 length array down to 2
        map = map.remove(k1);
        assertEquals(2, map.size());

        // Remove from a 2 length array down to 1 (should unwrap from CollisionNode)
        map = map.remove(k3);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(k4));

        // Remove last
        map = map.remove(k4);
        assertTrue(map.isEmpty());
    }

    @Test
    void testCollisionNodeBranchCoverage() {
        CollidingKey k1 = new CollidingKey(1);
        CollidingKey k2 = new CollidingKey(2);
        CollidingKey k3NotInMap = new CollidingKey(3); // Same hash, not inserted

        // A key with a completely different hash
        CollidingKey kDifferentHash = new CollidingKey(99) {
            @Override
            public int hashCode() {
                return 999;
            }
        };

        // Create a map with a CollisionNode at the root.
        // Because k1 is inserted first, mergeLeaves puts k1 at index 0 and k2 at index 1.
        PersistentHashMap<CollidingKey, String> map = PersistentHashMap.<CollidingKey, String>empty()
                .put(k1, "A").put(k2, "B");

        // --- Cover CollisionNode.get() missing branches ---
        // Branch: hash == h (TRUE), but loop finishes without matching key
        assertEquals(Optional.empty(), map.get(k3NotInMap));

        // Branch: hash != h (FALSE)
        assertEquals(Optional.empty(), map.get(kDifferentHash));

        // --- Cover CollisionNode.put() missing branches ---
        // Branch: put a key with a DIFFERENT hash into an existing CollisionNode.
        // This forces the CollisionNode to merge with the new LeafNode, pushing both
        // down into a newly created BitmapIndexedNode.
        PersistentHashMap<CollidingKey, String> mapAfterDiffPut = map.put(kDifferentHash, "C");
        assertEquals(3, mapAfterDiffPut.size());
        assertEquals(Optional.of("C"), mapAfterDiffPut.get(kDifferentHash));
        assertEquals(Optional.of("A"), mapAfterDiffPut.get(k1));
        assertEquals(Optional.of("B"), mapAfterDiffPut.get(k2));

        // --- Cover CollisionNode.remove() missing branches ---
        // Branch: hash == h (TRUE), but loop finishes without matching key
        PersistentHashMap<CollidingKey, String> mapAfterK3Remove = map.remove(k3NotInMap);
        assertSame(map, mapAfterK3Remove, "Removing a non-existent key with the same hash should return self");

        // Branch: hash != h (FALSE)
        PersistentHashMap<CollidingKey, String> mapAfterDiffRemove = map.remove(kDifferentHash);
        assertSame(map, mapAfterDiffRemove, "Removing a key with a different hash should return self");

        // --- Cover CollisionNode Collapse Ternary (i == 0 ? leaves[1] : leaves[0]) ---
        // Ternary True (i == 0): Remove k1 (which is at index 0). Should return k2.
        PersistentHashMap<CollidingKey, String> mapAfterK1Remove = map.remove(k1);
        assertEquals(1, mapAfterK1Remove.size());
        assertEquals(Optional.of("B"), mapAfterK1Remove.get(k2));
        assertFalse(mapAfterK1Remove.containsKey(k1));

        // Ternary False (i == 1): Remove k2 (which is at index 1). Should return k1.
        PersistentHashMap<CollidingKey, String> mapAfterK2Remove = map.remove(k2);
        assertEquals(1, mapAfterK2Remove.size());
        assertEquals(Optional.of("A"), mapAfterK2Remove.get(k1));
        assertFalse(mapAfterK2Remove.containsKey(k2));
    }

    @Test
    void testGetHashThrowsOnInvalidNode() throws Exception {
        // 1. Resolve the private Node interface
        Class<?> nodeInterface = Class.forName("persistent_data_structures.PersistentHashMap$Node");

        // 2. Resolve the private getHash method and make it accessible
        Method getHashMethod = PersistentHashMap.class.getDeclaredMethod("getHash", nodeInterface);
        getHashMethod.setAccessible(true);

        // 3. Resolve the private EmptyNode class and get its INSTANCE field
        Class<?> emptyNodeClass = Class.forName("persistent_data_structures.PersistentHashMap$EmptyNode");
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
    void testEmptyMapIteratorCoverage() {
        PersistentHashMap<Integer, String> map = PersistentHashMap.empty();
        Iterator<Map.Entry<Integer, String>> it = map.iterator();

        assertFalse(it.hasNext(), "Empty map iterator should not have next");
        NoSuchElementException nsee = assertThrows(NoSuchElementException.class, it::next, "Calling next() on empty map iterator throws");
        assertNotNull(nsee);
    }

    @Test
    void testBitmapIndexedNodeReturnsSelf() {
        // We need keys with different hashes to force the creation of a BitmapIndexedNode.
        CollidingKey k1 = new CollidingKey(1); // Hash is 42
        CollidingKey k2 = new CollidingKey(2); // Hash is 42 (Collides with k1)

        CollidingKey kDifferent = new CollidingKey(99) {
            @Override
            public int hashCode() {
                return 999;
            } // Different hash
        };

        // 1. Create a map with a BitmapIndexedNode at the root.
        // It has two branches: one for hash 42 (LeafNode for k1), one for hash 999 (LeafNode for kDifferent).
        PersistentHashMap<CollidingKey, String> map = PersistentHashMap.<CollidingKey, String>empty()
                .put(k1, "A")
                .put(kDifferent, "B");

        // --- Cover BitmapIndexedNode.put() -> if (newChild == child) return this; ---
        // Put the exact same key-value pair for an existing key.
        // The BitmapIndexedNode routes the put to the LeafNode(k1).
        // The LeafNode sees no changes and returns itself.
        // The BitmapIndexedNode sees (newChild == child) and returns itself.
        PersistentHashMap<CollidingKey, String> mapAfterSamePut = map.put(k1, "A");
        assertSame(map, mapAfterSamePut, "Putting an identical key-value through a BitmapIndexedNode should return self");

        // --- Cover BitmapIndexedNode.remove() -> if (newChild == child) return this; ---
        // Remove a key that does NOT exist, but whose hash shares a path with an existing key.
        // k2 has hash 42, so it traverses into the exact same branch as k1.
        // The LeafNode(k1) sees the key doesn't match and returns itself.
        // The BitmapIndexedNode sees (newChild == child) and returns itself.
        PersistentHashMap<CollidingKey, String> mapAfterMissingRemove = map.remove(k2);
        assertSame(map, mapAfterMissingRemove, "Removing a missing key that routes through an existing BitmapIndexedNode branch should return self");
    }

    @Test
    void testIteratorThrowsOnInvalidNode() throws Exception {
        // 1. Resolve the private Node interface
        Class<?> nodeInterface = Class.forName("persistent_data_structures.PersistentHashMap$Node");

        // 2. Create a dynamic proxy to act as a rogue Node implementation
        Object bogusNode = java.lang.reflect.Proxy.newProxyInstance(
                PersistentHashMap.class.getClassLoader(),
                new Class<?>[]{nodeInterface},
                (proxy, method, args) -> null
        );

        // 3. Create a new map instance with the bogus node via reflection.
        // (We construct a new one rather than mutating PersistentHashMap.empty()
        // to avoid breaking the singleton for other tests).
        java.lang.reflect.Constructor<?> constructor = PersistentHashMap.class.getDeclaredConstructor(int.class, nodeInterface);
        constructor.setAccessible(true);

        @SuppressWarnings("unchecked")
        PersistentHashMap<Integer, String> bogusMap = (PersistentHashMap<Integer, String>) constructor.newInstance(1, bogusNode);

        // 4. Calling iterator() triggers the instance initializer, which calls advance() and hits the exception
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                bogusMap::iterator
        );

        assertTrue(exception.getMessage().contains("Unknown node type in iterator"));
    }

    // --- Iterator & Utility Tests ---
    @Test
    void testIteratorExhaustion() {
        PersistentHashMap<Integer, String> map = PersistentHashMap.<Integer, String>empty()
                .put(1, "A").put(2, "B");

        Iterator<Map.Entry<Integer, String>> it = map.iterator();

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
        CollidingKey k1 = new CollidingKey(1);
        CollidingKey k2 = new CollidingKey(2);

        PersistentHashMap<CollidingKey, String> map = PersistentHashMap.<CollidingKey, String>empty()
                .put(k1, "A").put(k2, "B");

        int count = 0;
        for (@SuppressWarnings("unused") Map.Entry<CollidingKey, String> ignored : map) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void testEqualsAndHashCode() {
        PersistentHashMap<Integer, String> map1 = PersistentHashMap.<Integer, String>empty().put(1, "A").put(2, "B").put(3, "C");
        PersistentHashMap<Integer, String> map2 = PersistentHashMap.<Integer, String>empty().put(3, "C").put(1, "A").put(2, "B");
        PersistentHashMap<Integer, String> map3 = PersistentHashMap.<Integer, String>empty().put(1, "A").put(3, "C");
        PersistentHashMap<Integer, String> map4 = PersistentHashMap.<Integer, String>empty().put(1, "A").put(2, "Z").put(3, "C");

        // NEW: Same size as map1, but contains a different key (4 instead of 3).
        // This forces the loop to look up key '3', get an Optional.empty(), and trigger the missing branch.
        PersistentHashMap<Integer, String> map5 = PersistentHashMap.<Integer, String>empty().put(1, "A").put(2, "B").put(4, "C");

        assertEquals(map1, map1); // self
        assertEquals(map1, map2); // same mappings, different insertion order
        assertNotEquals(map1, map3); // different size
        assertNotEquals(map1, map4); // same keys, different values
        assertNotEquals(map1, map5); // same size, different keys (Covers thatValue.isEmpty() == true)
        assertNotEquals(map1, new Object()); // different type
        assertNotEquals(map1, null);

        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    void testToString() {
        PersistentHashMap<Integer, String> emptyMap = PersistentHashMap.empty();
        assertEquals("{}", emptyMap.toString());

        PersistentHashMap<Integer, String> map1 = PersistentHashMap.<Integer, String>empty().put(1, "A");
        assertEquals("{1=A}", map1.toString());

        PersistentHashMap<Integer, String> map2 = map1.put(2, "B");
        String str = map2.toString();
        // HAMT doesn't guarantee order, so it could be {1=A, 2=B} or {2=B, 1=A}
        assertTrue(str.equals("{1=A, 2=B}") || str.equals("{2=B, 1=A}"));
    }

    // --- Serialization Proxy Tests ---
    @Test
    void testSerializationRoundTrip() throws Exception {
        PersistentHashMap<Integer, String> original = PersistentHashMap.<Integer, String>empty()
                .put(1, "A")
                .put(2, "B")
                .put(3, "C");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PersistentHashMap<Integer, String> deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            @SuppressWarnings("unchecked")
            PersistentHashMap<Integer, String> casted = (PersistentHashMap<Integer, String>) ois.readObject();
            deserialized = casted;
        }

        assertNotSame(original, deserialized);
        assertEquals(original, deserialized);
        assertEquals(original.size(), deserialized.size());
    }

    @Test
    void testDirectReadObjectThrowsException() throws Exception {
        PersistentHashMap<Integer, String> map = PersistentHashMap.empty();

        // Use reflection to bypass visibility and invoke readObject directly to guarantee 100% line coverage
        Method readObjectMethod = PersistentHashMap.class.getDeclaredMethod("readObject", ObjectInputStream.class);
        readObjectMethod.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> readObjectMethod.invoke(map, (ObjectInputStream) null)
        );

        assertTrue(exception.getCause() instanceof java.io.InvalidObjectException);
        assertEquals("Serialization proxy required", exception.getCause().getMessage());
    }

    /**
     * A helper class that deliberately forces hash collisions to ensure full
     * coverage of the CollisionNode logic.
     */
    static class CollidingKey {

        final int id;

        CollidingKey(int id) {
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
            if (!(o instanceof CollidingKey)) {
                return false;
            }
            return id == ((CollidingKey) o).id;
        }

        @Override
        public String toString() {
            return "K" + id;
        }
    }
}
