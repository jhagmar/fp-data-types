package persistent_data_structures;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class PersistentVectorTest {

    @Test
    void testEmptyVector() {
        PersistentVector<String> empty = PersistentVector.empty();
        assertTrue(empty.isEmpty(), "Empty vector should be empty");
        assertEquals(0, empty.size(), "Empty vector size should be 0");
        assertEquals("[]", empty.toString(), "toString should return []");
        assertFalse(empty.iterator().hasNext(), "Iterator of empty vector should have no next element");
        IllegalStateException ise = assertThrows(IllegalStateException.class, empty::pop, "Popping empty vector should throw");
        assertEquals("Cannot pop from an empty vector", ise.getMessage());
        IndexOutOfBoundsException ioobe = assertThrows(IndexOutOfBoundsException.class, () -> empty.get(0), "Getting from empty vector should throw");
        assertEquals("Index: 0, Size: 0", ioobe.getMessage());
    }

    @Test
    void testOfVarargs() {
        PersistentVector<Integer> vector = PersistentVector.of(1, 2, 3);
        assertFalse(vector.isEmpty());
        assertEquals(3, vector.size());
        assertEquals(1, vector.get(0));
        assertEquals(2, vector.get(1));
        assertEquals(3, vector.get(2));
    }

    @Test
    void testAppendAndGetWithinTail() {
        PersistentVector<Integer> vector = PersistentVector.<Integer>empty();
        for (int i = 0; i < 32; i++) {
            vector = vector.append(i);
        }
        assertEquals(32, vector.size());
        for (int i = 0; i < 32; i++) {
            assertEquals(i, vector.get(i), "Failed at index " + i);
        }
    }

    @Test
    void testAppendPushToTree() {
        // Appending 33 elements forces the first 32 elements into the tree and 1 into the tail
        PersistentVector<Integer> vector = PersistentVector.<Integer>empty();
        for (int i = 0; i < 33; i++) {
            vector = vector.append(i);
        }
        assertEquals(33, vector.size());
        for (int i = 0; i < 33; i++) {
            assertEquals(i, vector.get(i), "Failed at index " + i);
        }
    }

    @Test
    void testAppendRootOverflow() {
        // Appending > 1024  (32 * 32) + 32 + 1 elements forces root overflow (shift increases)
        PersistentVector<Integer> vector = PersistentVector.<Integer>empty();
        int targetSize = 1057; // 1024 + 32 + 1 to ensure we hit root overflow
        for (int i = 0; i < targetSize; i++) {
            vector = vector.append(i);
        }
        assertEquals(targetSize, vector.size());
        for (int i = 0; i < targetSize; i++) {
            assertEquals(i, vector.get(i), "Failed at index " + i);
        }
    }

    @Test
    void testAppendDeepTree() {
        // Appending > 32768 (32 * 32 * 32) + 32 + 1 elements forces multiple levels of tree
        PersistentVector<Integer> vector = PersistentVector.<Integer>empty();
        int targetSize = 32801; // 32 * 32 * 32 + 32 + 1 to ensure we hit multiple levels
        for (int i = 0; i < targetSize; i++) {
            vector = vector.append(i);
        }
        assertEquals(targetSize, vector.size());
        for (int i = 0; i < targetSize; i++) {
            assertEquals(i, vector.get(i), "Failed at index " + i);
        }
    }

    @Test
    void testSetWithinTail() {
        PersistentVector<String> vector = PersistentVector.of("a", "b", "c");
        PersistentVector<String> modified = vector.set(1, "z");
        
        assertEquals("b", vector.get(1), "Original vector should remain unmodified");
        assertEquals("z", modified.get(1), "New vector should contain updated element");
        assertEquals(3, modified.size());
    }

    @Test
    void testSetWithinTree() {
        PersistentVector<Integer> vector = PersistentVector.<Integer>empty();
        for (int i = 0; i < 40; i++) {
            vector = vector.append(i);
        }
        
        PersistentVector<Integer> modified = vector.set(5, 999).set(35, 888);
        
        assertEquals(5, vector.get(5)); // original intact
        assertEquals(35, vector.get(35)); // original intact
        assertEquals(999, modified.get(5)); // tree node update successful
        assertEquals(888, modified.get(35)); // tail update successful
    }

    @Test
    void testPopWithinTail() {
        PersistentVector<Integer> vector = PersistentVector.of(1, 2, 3);
        PersistentVector<Integer> popped = vector.pop();
        
        assertEquals(2, popped.size());
        assertEquals(1, popped.get(0));
        assertEquals(2, popped.get(1));
        IndexOutOfBoundsException ioobe = assertThrows(IndexOutOfBoundsException.class, () -> popped.get(2));
        assertEquals("Index: 2, Size: 2", ioobe.getMessage());
    }

    @Test
    void testPopDownToEmpty() {
        PersistentVector<Integer> vector = PersistentVector.of(1).pop();
        assertTrue(vector.isEmpty());
        assertEquals(0, vector.size());
    }

    @Test
    void testPopTreeContraction() {
        // Go to 33 elements (1 in tail, 32 in tree)
        PersistentVector<Integer> vector = PersistentVector.<Integer>empty();
        for (int i = 0; i < 33; i++) {
            vector = vector.append(i);
        }
        
        // Pop the tail (now 32 elements, tail becomes empty or fetching from tree)
        PersistentVector<Integer> popped1 = vector.pop();
        assertEquals(32, popped1.size());
        assertEquals(31, popped1.get(31));

        // Pop again to force tree contraction logic
        PersistentVector<Integer> popped2 = popped1.pop();
        assertEquals(31, popped2.size());
        assertEquals(30, popped2.get(30));
    }
    
    @Test
    void testPopDeepTree() {
        // Test popping from a deep tree to hit recursive popTail logic
        PersistentVector<Integer> vector = PersistentVector.<Integer>empty();
        int targetSize = 32801; // 32 * 32 * 32 + 32 + 1 to ensure we hit multiple levels
        for (int i = 0; i < targetSize; i++) {
            vector = vector.append(i);
        }
        
        for (int i = targetSize - 1; i >= 0; i--) {
            assertEquals(i + 1, vector.size());
            assertEquals(i, vector.get(i));
            vector = vector.pop();
        }
        assertTrue(vector.isEmpty());
    }

    @Test
    void testBoundsChecking() {
        PersistentVector<String> vector = PersistentVector.of("a", "b");
        
        IndexOutOfBoundsException ioobe1 = assertThrows(IndexOutOfBoundsException.class, () -> vector.get(-1));
        IndexOutOfBoundsException ioobe2 = assertThrows(IndexOutOfBoundsException.class, () -> vector.get(2));
        assertEquals("Index: -1, Size: 2", ioobe1.getMessage());
        assertEquals("Index: 2, Size: 2", ioobe2.getMessage());
        
        IndexOutOfBoundsException ioobe3 = assertThrows(IndexOutOfBoundsException.class, () -> vector.set(-1, "x"));
        IndexOutOfBoundsException ioobe4 = assertThrows(IndexOutOfBoundsException.class, () -> vector.set(2, "x"));
        assertEquals("Index: -1, Size: 2", ioobe3.getMessage());
        assertEquals("Index: 2, Size: 2", ioobe4.getMessage());
    }

    @Test
    void testNullChecks() {
        PersistentVector<String> vector = PersistentVector.of("a");
        
        NullPointerException npe1 = assertThrows(NullPointerException.class, () -> vector.append(null));
        NullPointerException npe2 = assertThrows(NullPointerException.class, () -> vector.set(0, null));
        assertEquals("PersistentVector does not permit null elements", npe1.getMessage());
        assertEquals("PersistentVector does not permit null elements", npe2.getMessage());
        // of() varargs null check depends on append() which is handled above
    }

    @Test
    void testIterator() {
        PersistentVector<Integer> vector = PersistentVector.<Integer>empty();
        for (int i = 0; i < 70; i++) { // Spans across multiple blocks
            vector = vector.append(i);
        }

        Iterator<Integer> it = vector.iterator();
        for (int i = 0; i < 70; i++) {
            assertTrue(it.hasNext());
            assertEquals(i, it.next());
        }
        assertFalse(it.hasNext());
        NoSuchElementException nsee = assertThrows(NoSuchElementException.class, it::next);
        assertEquals("No such element", nsee.getMessage());
    }

    @Test
    void testToList() {
        PersistentVector<Integer> vector = PersistentVector.of(1, 2, 3);
        List<Integer> list = vector.toList();
        
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        UnsupportedOperationException uoe = assertThrows(UnsupportedOperationException.class, () -> list.add(4), "List should be unmodifiable");
        assertNotNull(uoe);
    }

    @Test
    void testEqualsAndHashCode() {
        PersistentVector<Integer> v1 = PersistentVector.of(1, 2, 3);
        PersistentVector<Integer> v2 = PersistentVector.of(1, 2, 3);
        PersistentVector<Integer> v3 = PersistentVector.of(1, 2, 4);
        PersistentVector<Integer> v4 = PersistentVector.of(1, 2);

        assertEquals(v1, v2);
        assertEquals(v1.hashCode(), v2.hashCode(), "Equal vectors must have equal hash codes");
        assertEquals(v1, v1); // reflexivity

        assertNotEquals(v1, v3);
        assertNotEquals(v1, v4);
        assertNotEquals(v1, null);
        assertNotEquals(v1, "A string");
    }

    @Test
    void testToString() {
        PersistentVector<Integer> vector = PersistentVector.of(1, 2, 3);
        assertEquals("[1, 2, 3]", vector.toString());
    }

    @Test
    void testSerialization() throws IOException, ClassNotFoundException {
        PersistentVector<Integer> vector = PersistentVector.<Integer>empty();
        for (int i = 0; i < 50; i++) {
            vector = vector.append(i);
        }

        // Serialize
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(vector);
        }

        // Deserialize
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        @SuppressWarnings("unchecked")
                PersistentVector<Integer> deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            deserialized = (PersistentVector<Integer>) ois.readObject();
        }

        // Verify
        assertEquals(vector.size(), deserialized.size());
        assertEquals(vector, deserialized);
        assertEquals(vector.hashCode(), deserialized.hashCode());
    }
}