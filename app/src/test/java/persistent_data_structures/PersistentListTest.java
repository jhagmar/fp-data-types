package persistent_data_structures;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PersistentListTest {

    @Test
    void testEmpty() {
        PersistentList<Integer> list = PersistentList.empty();
        assertTrue(list.isEmpty(), "Empty list should report isEmpty() == true");
        assertEquals(0, list.size(), "Empty list size should be 0");
        assertTrue(list.head().isEmpty(), "Empty list should have no head");
        assertTrue(list.tail().isEmpty(), "Empty list should have no tail");
    }

    @Test
    void testOfFactories() {
        PersistentList<String> list1 = PersistentList.of("A");
        assertEquals(1, list1.size(), "Size should be 1");
        assertEquals("A", list1.head().get(), "Head should be 'A'");

        PersistentList<String> list2 = PersistentList.of("A", "B");
        assertEquals(2, list2.size(), "Size should be 2");
        assertEquals("A", list2.head().get(), "Head should be 'A'");
        assertEquals("B", list2.tail().head().get(), "Second element should be 'B'");

        PersistentList<Integer> list3 = PersistentList.of(1, 2, 3, 4);
        assertEquals(4, list3.size(), "Varargs size should be 4");
        assertEquals(1, list3.head().get(), "Varargs head should be 1");

        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> PersistentList.of(1, null, 3),
                "Varargs should reject nulls");
        assertNotNull(npe, "assertThrows should return the caught exception");
    }

    @Test
    void testPrependAndStructuralSharing() {
        PersistentList<Integer> original = PersistentList.of(2, 3);
        PersistentList<Integer> prepended = original.prepend(1);

        assertEquals(3, prepended.size(), "Prepended list should grow by 1");
        assertEquals(1, prepended.head().get(), "New head should be 1");

        // Test Structural Sharing: The tail of the new list MUST be the exact instance of the original list
        assertSame(original, prepended.tail(), "Prepending must structurally share the existing tail");

        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> original.prepend(null),
                "Prepend should reject null");
        assertNotNull(npe, "assertThrows should return the caught exception");
    }

    @Test
    void testHeadAndTail() {
        PersistentList<Integer> list = PersistentList.of(10, 20);
        assertEquals(Optional.of(10), list.head(), "Head should return Optional[10]");

        PersistentList<Integer> tail = list.tail();
        assertEquals(1, tail.size(), "Tail should have size 1");
        assertEquals(Optional.of(20), tail.head(), "Tail's head should be 20");
    }

    @Test
    void testRemove() {
        PersistentList<String> list = PersistentList.of("A", "B", "C", "D");

        // Remove Head
        PersistentList<String> noHead = list.remove("A");
        assertEquals(PersistentList.of("B", "C", "D"), noHead, "Removing head should return tail");

        // Remove Middle
        PersistentList<String> noMiddle = list.remove("C");
        assertEquals(PersistentList.of("A", "B", "D"), noMiddle, "Removing middle element failed");

        // Remove Missing (Must return exact same instance)
        PersistentList<String> noChange = list.remove("Z");
        assertSame(list, noChange, "Removing missing element should return the exact original instance");

        // Remove from Empty
        PersistentList<String> empty = PersistentList.empty();
        assertSame(empty, empty.remove("A"), "Removing from empty should return empty instance");
    }

    @Test
    void testReverse() {
        PersistentList<Integer> list = PersistentList.of(1, 2, 3);
        PersistentList<Integer> reversed = list.reverse();
        assertEquals(PersistentList.of(3, 2, 1), reversed, "List did not reverse correctly");
        assertEquals(PersistentList.empty(), PersistentList.empty().reverse(), "Empty reverse should be empty");
    }

    @Test
    void testTake() {
        PersistentList<Integer> list = PersistentList.of(1, 2, 3, 4, 5);

        assertEquals(PersistentList.of(1, 2), list.take(2), "Take(2) failed");
        assertEquals(PersistentList.empty(), list.take(0), "Take(0) should be empty");
        assertEquals(PersistentList.empty(), list.take(-5), "Take(negative) should be empty");
        assertSame(list, list.take(10), "Take(>size) should return exact original instance");
    }

    @Test
    void testDrop() {
        PersistentList<Integer> list = PersistentList.of(1, 2, 3, 4, 5);

        assertEquals(PersistentList.of(3, 4, 5), list.drop(2), "Drop(2) failed");
        assertSame(list, list.drop(0), "Drop(0) should return exact original instance");
        assertSame(list, list.drop(-5), "Drop(negative) should return exact original instance");
        assertEquals(PersistentList.empty(), list.drop(10), "Drop(>size) should be empty");
    }

    @Test
    void testMap() {
        PersistentList<String> list = PersistentList.of("a", "b", "c");
        PersistentList<String> upper = list.map(String::toUpperCase);
        assertEquals(PersistentList.of("A", "B", "C"), upper, "Map failed to transform elements");
        assertEquals(PersistentList.empty(), PersistentList.empty().map(x -> x), "Map on empty should return empty");
    }

    @Test
    void testFilter() {
        PersistentList<Integer> list = PersistentList.of(1, 2, 3, 4, 5);
        PersistentList<Integer> evens = list.filter(n -> n % 2 == 0);
        assertEquals(PersistentList.of(2, 4), evens, "Filter failed to retain correct elements");
        assertEquals(PersistentList.empty(), PersistentList.empty().filter(x -> true), "Filter empty is empty");
    }

    @Test
    void testFoldLeft() {
        PersistentList<Integer> list = PersistentList.of(1, 2, 3, 4);
        int sum = list.foldLeft(0, Integer::sum);
        assertEquals(10, sum, "FoldLeft sum failed");

        PersistentList<String> strings = PersistentList.of("H", "e", "l", "l", "o");
        String concat = strings.foldLeft("", String::concat);
        assertEquals("Hello", concat, "FoldLeft concat failed");
    }

    @Test
    void testConcat() {
        PersistentList<Integer> l1 = PersistentList.of(1, 2);
        PersistentList<Integer> l2 = PersistentList.of(3, 4);

        assertEquals(PersistentList.of(1, 2, 3, 4), l1.concat(l2), "Concat failed");
        assertSame(l2, PersistentList.<Integer>empty().concat(l2), "Concat to empty should return other");
        assertSame(l1, l1.concat(PersistentList.empty()), "Concat empty should return this");
    }

    @Test
    void testToListImmutability() {
        PersistentList<Integer> list = PersistentList.of(1, 2, 3);
        List<Integer> javaList = list.toList();

        assertEquals(3, javaList.size(), "toList size mismatch");
        assertEquals(1, javaList.get(0), "toList first element mismatch");

        UnsupportedOperationException uoe = assertThrows(UnsupportedOperationException.class,
                () -> javaList.add(4),
                "toList should be unmodifiable");
        assertNotNull(uoe, "assertThrows should return the caught exception");
    }

    @Test
    void testIterator() {
        PersistentList<Integer> list = PersistentList.of(10, 20);
        Iterator<Integer> it = list.iterator();

        assertTrue(it.hasNext(), "Iterator should have next");
        assertEquals(10, it.next(), "First element should be 10");
        assertTrue(it.hasNext(), "Iterator should have next");
        assertEquals(20, it.next(), "Second element should be 20");
        assertFalse(it.hasNext(), "Iterator should be empty");

        NoSuchElementException nsee = assertThrows(NoSuchElementException.class,
                it::next,
                "Iterator should throw on exhaust");
        assertNotNull(nsee, "assertThrows should return the caught exception");
    }

    @Test
    void testEqualsAndHashCode() {
        PersistentList<Integer> l1 = PersistentList.of(1, 2, 3);
        PersistentList<Integer> l2 = PersistentList.of(1).concat(PersistentList.of(2, 3));
        PersistentList<Integer> l3 = PersistentList.of(1, 2, 4);

        assertEquals(l2, l1, "Identical lists should be equal");
        assertEquals(l2.hashCode(), l1.hashCode(), "Identical lists should have same hashCode");
        assertNotEquals(l3, l1, "Different lists should not be equal");
        assertNotEquals(null, l1, "List should not equal null");
        assertNotEquals("String", l1, "List should not equal different type");
    }

    @Test
    void testToString() {
        assertEquals("[]", PersistentList.empty().toString(), "Empty toString mismatch");
        assertEquals("[1]", PersistentList.of(1).toString(), "Single element toString mismatch");
        assertEquals("[1, 2, 3]", PersistentList.of(1, 2, 3).toString(), "Multiple element toString mismatch");
    }

    @Test
    void testSerialization() {
        PersistentList<String> original = PersistentList.of("Java", "21", "Records", "Serialization");
        PersistentList<String> deserialized = null;

        try {
            // Write to memory
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(original);
            }

            // Read from memory
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                @SuppressWarnings("unchecked")
                PersistentList<String> read = (PersistentList<String>) ois.readObject();
                deserialized = read;
            }

        } catch (IOException | ClassNotFoundException e) {
            fail("Serialization threw an exception: " + e.getMessage());
        }

        assertEquals(original, deserialized, "Deserialized list does not match original");
    }

    @Test
    void testEquals() {
        PersistentList<Integer> list1 = PersistentList.of(1, 2, 3);
        PersistentList<Integer> list2 = PersistentList.of(1, 2, 3);
        PersistentList<Integer> list3 = PersistentList.of(1, 2);

        assertEquals(list1, list2, "Lists with same elements should be equal");
        assertNotEquals(list1, list3, "Lists with different elements should not be equal");
        assertNotEquals(null, list1, "List should not equal null");
        assertNotEquals(list1, "String", "List should not equal different type");
    }

}