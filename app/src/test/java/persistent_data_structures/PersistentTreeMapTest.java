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

class PersistentTreeMapTest {

    @Test
    void testEmptyMap() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.empty();
        assertTrue(map.isEmpty());
        assertEquals(0, map.size());
        assertEquals(Optional.empty(), map.get(1));
        assertFalse(map.containsKey(1));
    }

    @Test
    void testPutAndGet() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
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
        PersistentTreeMap<Integer, String> map1 = PersistentTreeMap.empty();
        PersistentTreeMap<Integer, String> map2 = map1.put(1, "A");
        PersistentTreeMap<Integer, String> map3 = map2.put(2, "B");

        assertTrue(map1.isEmpty());
        assertEquals(1, map2.size());
        assertEquals(2, map3.size());

        assertFalse(map1.containsKey(1));
        assertTrue(map2.containsKey(1));
        assertTrue(map3.containsKey(2));
    }

    @Test
    void testPutUpdatesExistingValue() {
        PersistentTreeMap<Integer, String> map1 = PersistentTreeMap.<Integer, String>empty().put(1, "A");
        PersistentTreeMap<Integer, String> map2 = map1.put(1, "B");

        assertEquals(1, map1.size());
        assertEquals(1, map2.size());
        assertEquals(Optional.of("A"), map1.get(1));
        assertEquals(Optional.of("B"), map2.get(1));
    }

    @Test
    void testPutOptimizationSameValueReturnsSelf() {
        PersistentTreeMap<Integer, String> map1 = PersistentTreeMap.<Integer, String>empty().put(1, "A");
        PersistentTreeMap<Integer, String> map2 = map1.put(1, "A");

        assertSame(map1, map2, "Putting the exact same key-value pair should return the identical instance");
    }

    @Test
    void testNullKeysRejected() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.empty();
        NullPointerException exception1 = assertThrows(NullPointerException.class, () -> map.put(null, "A"));
        assertEquals("PersistentTreeMap does not permit null keys", exception1.getMessage(),
                "Exception message must exactly match the defined contract for null key rejection");
        NullPointerException exception2 = assertThrows(NullPointerException.class, () -> map.get(null));
        assertEquals("PersistentTreeMap does not permit null keys", exception2.getMessage(),
                "Exception message must exactly match the defined contract for null key rejection");
        NullPointerException exception3 = assertThrows(NullPointerException.class, () -> map.containsKey(null));
        assertEquals("PersistentTreeMap does not permit null keys", exception3.getMessage(),
                "Exception message must exactly match the defined contract for null key rejection");
        NullPointerException exception4 = assertThrows(NullPointerException.class, () -> map.remove(null));
        assertEquals("PersistentTreeMap does not permit null keys", exception4.getMessage(),
                "Exception message must exactly match the defined contract for null key rejection");
    }

    @Test
    void testNullValuesRejected() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.empty();
        NullPointerException exception = assertThrows(NullPointerException.class, () -> map.put(1, null));
        assertEquals("PersistentTreeMap does not permit null values", exception.getMessage(),
                "Exception message must exactly match the defined contract for null value rejection");
    }

    @Test
    void testAvlBalanceLeftLeft() {
        // Triggers LL rotation (Right Rotation)
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(3, "C").put(2, "B").put(1, "A");
        assertEquals(3, map.size());
        assertTrue(map.containsKey(1) && map.containsKey(2) && map.containsKey(3));
    }

    @Test
    void testAvlBalanceRightRight() {
        // Triggers RR rotation (Left Rotation)
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(1, "A").put(2, "B").put(3, "C");
        assertEquals(3, map.size());
        assertTrue(map.containsKey(1) && map.containsKey(2) && map.containsKey(3));
    }

    @Test
    void testAvlBalanceLeftRight() {
        // Triggers LR rotation (Left then Right Rotation)
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(3, "C").put(1, "A").put(2, "B");
        assertEquals(3, map.size());
        assertTrue(map.containsKey(1) && map.containsKey(2) && map.containsKey(3));
    }

    @Test
    void testAvlBalanceRightLeft() {
        // Triggers RL rotation (Right then Left Rotation)
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(1, "A").put(3, "C").put(2, "B");
        assertEquals(3, map.size());
        assertTrue(map.containsKey(1) && map.containsKey(2) && map.containsKey(3));
    }

    @Test
    void testPutIdenticalLeft() {
        // Inserting an identical key-value pair on the left subtree should return the same instance without modification
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(2, "B").put(1, "A").put(3, "C");
        PersistentTreeMap<Integer, String> result = map.put(1, "A");

        assertSame(map, result, "Putting the same key-value pair should yield an equal map");
    }

    @Test
    void testPutIdenticalRight() {
        // Inserting an identical key-value pair on the right subtree should return the same instance without modification
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(2, "B").put(1, "A").put(3, "C");
        PersistentTreeMap<Integer, String> result = map.put(3, "C");

        assertSame(map, result, "Putting the same key-value pair should yield an equal map");
    }

    @Test
    void removeNonExistentKeyReturnsSameInstance() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty().put(2, "B").put(3, "C").put(4, "D");

        PersistentTreeMap<Integer, String> result = map.remove(1);
        assertSame(map, result, "Removing a non-existent key should return the same instance without modification");

        result = map.remove(5);
        assertSame(map, result, "Removing a non-existent key should return the same instance without modification");
    }

    @Test
    void testRemoveNonExistentKeyReturnsSelf() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty().put(1, "A");
        PersistentTreeMap<Integer, String> map2 = map.remove(2);
        assertSame(map, map2);
    }

    @Test
    void testRemoveLeafNode() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(2, "B").put(1, "A").put(3, "C");
        PersistentTreeMap<Integer, String> result = map.remove(1);

        assertEquals(2, result.size());
        assertFalse(result.containsKey(1));
        assertTrue(result.containsKey(2));
        assertTrue(result.containsKey(3));
    }

    @Test
    void testRemoveNodeWithOneChild() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(5, "E").put(3, "C").put(8, "H").put(2, "B"); // 3 has left child 2
        PersistentTreeMap<Integer, String> result = map.remove(3);

        assertEquals(3, result.size());
        assertFalse(result.containsKey(3));
        assertTrue(result.containsKey(2));
    }

    @Test
    void testRemoveNodeWithTwoChildren() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(5, "E").put(3, "C").put(8, "H").put(7, "G").put(9, "I"); // 8 has children 7 and 9

        PersistentTreeMap<Integer, String> result = map.remove(8); // Replaced by successor 9

        assertEquals(4, result.size());
        assertFalse(result.containsKey(8));
        assertTrue(result.containsKey(5) && result.containsKey(3) && result.containsKey(7) && result.containsKey(9));
    }

    @Test
    void testRemoveRootCausesRebalance() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(4, "D").put(2, "B").put(6, "F").put(1, "A").put(3, "C").put(5, "E").put(7, "G");

        PersistentTreeMap<Integer, String> result = map.remove(4);
        assertEquals(6, result.size());
        assertFalse(result.containsKey(4));
    }

    @Test
    void testIteratorInOrderTraversal() {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(3, "C").put(1, "A").put(4, "D").put(2, "B");

        Iterator<Map.Entry<Integer, String>> it = map.iterator();

        assertTrue(it.hasNext());
        assertEquals(1, it.next().getKey());
        assertEquals(2, it.next().getKey());
        assertEquals(3, it.next().getKey());
        assertEquals(4, it.next().getKey());
        assertFalse(it.hasNext());

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, it::next);
        assertNotNull(exception, "Calling next() on an exhausted iterator should throw NoSuchElementException");
    }

    @Test
    void testEqualsAndHashCode() {
        PersistentTreeMap<Integer, String> map1 = PersistentTreeMap.<Integer, String>empty().put(1, "A").put(2, "B");
        PersistentTreeMap<Integer, String> map2 = PersistentTreeMap.<Integer, String>empty().put(2, "B").put(1, "A");
        PersistentTreeMap<Integer, String> map3 = PersistentTreeMap.<Integer, String>empty().put(1, "A").put(3, "C");
        PersistentTreeMap<Integer, String> map4 = PersistentTreeMap.<Integer, String>empty().put(1, "A");
        PersistentTreeMap<Integer, String> map5 = PersistentTreeMap.<Integer, String>empty().put(1, "A").put(2, "C");

        assertEquals(map1, map1); // self
        assertEquals(map1, map2); // same mappings, different insertion order
        assertNotEquals(map1, map3); // different values
        assertNotEquals(map1, map4); // different size
        assertNotEquals(map1, new Object()); // different type
        assertNotEquals(map1, map5); // same keys, different values
        assertNotEquals(map1, null);

        assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    void testToString() {
        PersistentTreeMap<Integer, String> emptyMap = PersistentTreeMap.empty();
        assertEquals("{}", emptyMap.toString());

        PersistentTreeMap<Integer, String> map = PersistentTreeMap.<Integer, String>empty()
                .put(2, "B").put(1, "A");

        // Iteration order is guaranteed sorted by key
        assertEquals("{1=A, 2=B}", map.toString());
    }

    @Test
    void testSerializationRoundTrip() throws Exception {
        PersistentTreeMap<Integer, String> original = PersistentTreeMap.<Integer, String>empty()
                .put(1, "A")
                .put(2, "B")
                .put(3, "C");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PersistentTreeMap<Integer, String> deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            @SuppressWarnings("unchecked")
            PersistentTreeMap<Integer, String> casted = (PersistentTreeMap<Integer, String>) ois.readObject();
            deserialized = casted;
        }

        assertNotSame(original, deserialized);
        assertEquals(original, deserialized);
        assertEquals(original.size(), deserialized.size());
    }

    @Test
    void testDirectReadObjectThrowsException() throws Exception {
        PersistentTreeMap<Integer, String> map = PersistentTreeMap.empty();

        // Use reflection to bypass visibility and invoke readObject directly to guarantee 100% line coverage
        Method readObjectMethod = PersistentTreeMap.class.getDeclaredMethod("readObject", ObjectInputStream.class);
        readObjectMethod.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> readObjectMethod.invoke(map, (ObjectInputStream) null)
        );

        assertTrue(exception.getCause() instanceof java.io.InvalidObjectException);
        assertEquals("Serialization proxy required", exception.getCause().getMessage());
    }
}