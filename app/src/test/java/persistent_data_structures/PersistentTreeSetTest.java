package persistent_data_structures;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class PersistentTreeSetTest {

    @Test
    void testEmptySet() {
        PersistentTreeSet<Integer> set = PersistentTreeSet.empty();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        assertFalse(set.contains(1));
    }

    @Test
    void testAddAndContains() {
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(1)
                .add(2)
                .add(3);

        assertFalse(set.isEmpty());
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertFalse(set.contains(4));
    }

    @Test
    void testAddImmutability() {
        PersistentTreeSet<Integer> set1 = PersistentTreeSet.empty();
        PersistentTreeSet<Integer> set2 = set1.add(1);
        PersistentTreeSet<Integer> set3 = set2.add(2);

        assertTrue(set1.isEmpty());
        assertEquals(1, set2.size());
        assertEquals(2, set3.size());

        assertFalse(set1.contains(1));
        assertTrue(set2.contains(1));
        assertFalse(set2.contains(2));
        assertTrue(set3.contains(2));
    }

    @Test
    void testAddDuplicateReturnsSelf() {
        PersistentTreeSet<Integer> set1 = PersistentTreeSet.<Integer>empty().add(1);
        PersistentTreeSet<Integer> set2 = set1.add(1);

        assertSame(set1, set2, "Adding an existing element should return the identical instance");
        assertEquals(1, set1.size());
    }

    @Test
    void testNullElementsRejected() {
        PersistentTreeSet<Integer> set = PersistentTreeSet.empty();
        
        NullPointerException exception1 = assertThrows(NullPointerException.class, () -> set.add(null));
        assertEquals("PersistentTreeSet does not permit null elements", exception1.getMessage(), 
                "Exception message must exactly match the defined contract for null rejection");
                
        NullPointerException exception2 = assertThrows(NullPointerException.class, () -> set.contains(null));
        assertEquals("PersistentTreeSet does not permit null elements", exception2.getMessage(), 
                "Exception message must exactly match the defined contract for null rejection");
                
        NullPointerException exception3 = assertThrows(NullPointerException.class, () -> set.remove(null));
        assertEquals("PersistentTreeSet does not permit null elements", exception3.getMessage(), 
                "Exception message must exactly match the defined contract for null rejection");
    }

    @Test
    void testAvlBalanceLeftLeft() {
        // Triggers LL rotation (Right Rotation)
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(3).add(2).add(1);
        assertEquals(3, set.size());
        assertTrue(set.contains(1) && set.contains(2) && set.contains(3));
    }

    @Test
    void testAvlBalanceRightRight() {
        // Triggers RR rotation (Left Rotation)
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(1).add(2).add(3);
        assertEquals(3, set.size());
        assertTrue(set.contains(1) && set.contains(2) && set.contains(3));
    }

    @Test
    void testAvlBalanceLeftRight() {
        // Triggers LR rotation (Left then Right Rotation)
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(3).add(1).add(2);
        assertEquals(3, set.size());
        assertTrue(set.contains(1) && set.contains(2) && set.contains(3));
    }

    @Test
    void testAvlBalanceRightLeft() {
        // Triggers RL rotation (Right then Left Rotation)
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(1).add(3).add(2);
        assertEquals(3, set.size());
        assertTrue(set.contains(1) && set.contains(2) && set.contains(3));
    }

    @Test
    void testAddIdenticalLeft() {
        // Adding an identical element on the left subtree should return the same instance
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(2).add(1).add(3);
        PersistentTreeSet<Integer> result = set.add(1);
        
        assertSame(set, result, "Adding the same element should yield an equal set");
    }

    @Test
    void testAddIdenticalRight() {
        // Adding an identical element on the right subtree should return the same instance
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(2).add(1).add(3);
        PersistentTreeSet<Integer> result = set.add(3);
        
        assertSame(set, result, "Adding the same element should yield an equal set");
    }

    @Test
    void removeNonExistentElementReturnsSameInstance() {
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty().add(2).add(3).add(4);

        PersistentTreeSet<Integer> result = set.remove(1);
        assertSame(set, result, "Removing a non-existent element should return the same instance without modification");

        result = set.remove(5);
        assertSame(set, result, "Removing a non-existent element should return the same instance without modification");
    }

    @Test
    void testRemoveLeafNode() {
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(2).add(1).add(3);
        PersistentTreeSet<Integer> result = set.remove(1);
        
        assertEquals(2, result.size());
        assertFalse(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    void testRemoveNodeWithOneChild() {
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(5).add(3).add(8).add(2); // 3 has left child 2
        PersistentTreeSet<Integer> result = set.remove(3);

        assertEquals(3, result.size());
        assertFalse(result.contains(3));
        assertTrue(result.contains(2));
    }

    @Test
    void testRemoveNodeWithTwoChildren() {
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(5).add(3).add(8).add(7).add(9); // 8 has children 7 and 9
        
        PersistentTreeSet<Integer> result = set.remove(8); // Replaced by successor 9
        
        assertEquals(4, result.size());
        assertFalse(result.contains(8));
        assertTrue(result.contains(5) && result.contains(3) && result.contains(7) && result.contains(9));
    }

    @Test
    void testRemoveRootCausesRebalance() {
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(4).add(2).add(6).add(1).add(3).add(5).add(7);
        
        PersistentTreeSet<Integer> result = set.remove(4);
        assertEquals(6, result.size());
        assertFalse(result.contains(4));
    }

    @Test
    void testIteratorInOrderTraversal() {
        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(3).add(1).add(4).add(2);

        Iterator<Integer> it = set.iterator();
        
        assertTrue(it.hasNext());
        assertEquals(1, it.next());
        assertEquals(2, it.next());
        assertEquals(3, it.next());
        assertEquals(4, it.next());
        assertFalse(it.hasNext());
        
        NoSuchElementException exception = assertThrows(NoSuchElementException.class, it::next);
        assertNotNull(exception, "Calling next() on an exhausted iterator should throw NoSuchElementException");
    }

    @Test
    void testEqualsAndHashCode() {
        PersistentTreeSet<Integer> set1 = PersistentTreeSet.<Integer>empty().add(1).add(2);
        PersistentTreeSet<Integer> set2 = PersistentTreeSet.<Integer>empty().add(2).add(1);
        PersistentTreeSet<Integer> set3 = PersistentTreeSet.<Integer>empty().add(1).add(3);
        PersistentTreeSet<Integer> set4 = PersistentTreeSet.<Integer>empty().add(1);

        assertEquals(set1, set1); // self
        assertEquals(set1, set2); // same elements, different insertion order
        assertNotEquals(set1, set3); // different elements
        assertNotEquals(set1, set4); // different size
        assertNotEquals(set1, new Object()); // different type
        assertNotEquals(set1, null);

        // General contract for Set states hash code is the sum of its elements' hash codes
        assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Test
    void testToString() {
        PersistentTreeSet<Integer> emptySet = PersistentTreeSet.empty();
        assertEquals("[]", emptySet.toString());

        PersistentTreeSet<Integer> set = PersistentTreeSet.<Integer>empty()
                .add(2).add(1);
        
        // Iteration order is guaranteed sorted
        assertEquals("[1, 2]", set.toString());
    }

    @Test
    void testSerializationRoundTrip() throws Exception {
        PersistentTreeSet<Integer> original = PersistentTreeSet.<Integer>empty()
                .add(1)
                .add(2)
                .add(3);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PersistentTreeSet<Integer> deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            @SuppressWarnings("unchecked")
            PersistentTreeSet<Integer> casted = (PersistentTreeSet<Integer>) ois.readObject();
            deserialized = casted;
        }

        assertNotSame(original, deserialized);
        assertEquals(original, deserialized);
        assertEquals(original.size(), deserialized.size());
    }

    @Test
    void testDirectReadObjectThrowsException() throws Exception {
        PersistentTreeSet<Integer> set = PersistentTreeSet.empty();
        
        // Use reflection to bypass visibility and invoke readObject directly to guarantee 100% line coverage
        Method readObjectMethod = PersistentTreeSet.class.getDeclaredMethod("readObject", ObjectInputStream.class);
        readObjectMethod.setAccessible(true);
        
        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class, 
                () -> readObjectMethod.invoke(set, (ObjectInputStream) null)
        );
        
        assertTrue(exception.getCause() instanceof java.io.InvalidObjectException);
        assertEquals("Serialization proxy required", exception.getCause().getMessage());
    }
}