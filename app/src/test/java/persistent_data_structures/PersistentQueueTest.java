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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

class PersistentQueueTest {

    @Test
    void testEmpty() {
        PersistentQueue<Integer> queue = PersistentQueue.empty();
        assertTrue(queue.isEmpty(), "Empty queue should report isEmpty() == true");
        assertEquals(0, queue.size(), "Empty queue size should be 0");
        assertTrue(queue.peek().isEmpty(), "Empty queue should have no front element");
        assertSame(queue, queue.dequeue(), "Dequeueing an empty queue should return itself");
    }

    @Test
    void testOfFactories() {
        PersistentQueue<String> q1 = PersistentQueue.of("A");
        assertEquals(1, q1.size(), "Size should be 1");
        assertEquals("A", q1.peek().get(), "Front should be 'A'");

        PersistentQueue<String> q2 = PersistentQueue.of("A", "B");
        assertEquals(2, q2.size(), "Size should be 2");
        assertEquals("A", q2.peek().get(), "Front should be 'A'");
        assertEquals("B", q2.dequeue().peek().get(), "Next element should be 'B'");

        PersistentQueue<Integer> q3 = PersistentQueue.of(1, 2, 3, 4);
        assertEquals(4, q3.size(), "Varargs size should be 4");
        assertEquals(1, q3.peek().get(), "Varargs front should be 1");
        assertEquals(4, q3.dequeue().dequeue().dequeue().peek().get(), "Last element should be 4");

        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> PersistentQueue.of(1, null, 3),
                "Varargs should reject nulls");
        assertNotNull(npe, "assertThrows should return the caught exception");
    }

    @Test
    void testEnqueue() {
        PersistentQueue<Integer> q0 = PersistentQueue.empty();
        PersistentQueue<Integer> q1 = q0.enqueue(10);
        PersistentQueue<Integer> q2 = q1.enqueue(20);

        assertEquals(1, q1.size(), "Queue size should be 1");
        assertEquals(10, q1.peek().get(), "Front should be 10");

        assertEquals(2, q2.size(), "Queue size should grow to 2");
        assertEquals(10, q2.peek().get(), "Front should STILL be 10 (FIFO)");

        NullPointerException npe = assertThrows(NullPointerException.class,
                () -> q1.enqueue(null),
                "Enqueue should reject null");
        assertNotNull(npe, "assertThrows should return the caught exception");
    }

    @Test
    void testDequeueAndRebalancing() {
        // Enqueueing to empty immediately puts '1' in front.
        // Subsequent enqueues go to the rear list.
        PersistentQueue<Integer> queue = PersistentQueue.<Integer>empty()
                .enqueue(1)
                .enqueue(2)
                .enqueue(3);

        assertEquals(3, queue.size(), "Size should be 3");
        assertEquals(1, queue.peek().get(), "Front should be 1");

        // First dequeue removes the front.
        // Rebalance triggers here: rear (3, 2) is reversed to (2, 3) and becomes new front.
        PersistentQueue<Integer> rebalancedQueue = queue.dequeue();
        assertEquals(2, rebalancedQueue.size(), "Size should decrease to 2");
        assertEquals(2, rebalancedQueue.peek().get(), "New front should be 2");

        // Second dequeue
        PersistentQueue<Integer> singleItemQueue = rebalancedQueue.dequeue();
        assertEquals(1, singleItemQueue.size(), "Size should decrease to 1");
        assertEquals(3, singleItemQueue.peek().get(), "New front should be 3");

        // Final dequeue
        PersistentQueue<Integer> emptyQueue = singleItemQueue.dequeue();
        assertTrue(emptyQueue.isEmpty(), "Queue should be empty");
    }

    @Test
    void testMap() {
        PersistentQueue<String> queue = PersistentQueue.of("a", "b", "c");
        PersistentQueue<String> upperQueue = queue.map(String::toUpperCase);

        assertEquals(3, upperQueue.size(), "Mapped queue should retain size");
        assertEquals("A", upperQueue.peek().get(), "Mapped queue front is incorrect");
        assertEquals("B", upperQueue.dequeue().peek().get(), "Mapped queue second element is incorrect");
        assertEquals("C", upperQueue.dequeue().dequeue().peek().get(), "Mapped queue third element is incorrect");

        assertEquals(PersistentQueue.empty(), PersistentQueue.<String>empty().map(String::toUpperCase), "Map on empty should return empty");
    }

    @Test
    void testFilter() {
        PersistentQueue<Integer> queue = PersistentQueue.of(1, 2, 3, 4, 5, 6);
        PersistentQueue<Integer> evens = queue.filter(n -> n % 2 == 0);

        assertEquals(3, evens.size(), "Filtered queue should have 3 elements");
        assertEquals(2, evens.peek().get(), "First even should be 2");
        assertEquals(4, evens.dequeue().peek().get(), "Second even should be 4");
        assertEquals(6, evens.dequeue().dequeue().peek().get(), "Third even should be 6");

        assertEquals(PersistentQueue.empty(), PersistentQueue.<Integer>empty().filter(x -> true), "Filter empty is empty");
    }

    @Test
    void testFoldLeft() {
        PersistentQueue<Integer> queue = PersistentQueue.of(1, 2, 3, 4);
        int sum = queue.foldLeft(0, Integer::sum);
        assertEquals(10, sum, "FoldLeft sum failed to traverse front and rear correctly");

        PersistentQueue<String> strings = PersistentQueue.of("F", "I", "F", "O");
        String concat = strings.foldLeft("", String::concat);
        assertEquals("FIFO", concat, "FoldLeft must respect FIFO order");
    }

    @Test
    void testToListImmutability() {
        PersistentQueue<Integer> queue = PersistentQueue.of(1, 2, 3);
        List<Integer> javaList = queue.toList();

        assertEquals(3, javaList.size(), "toList size mismatch");
        assertEquals(1, javaList.get(0), "toList first element mismatch");
        assertEquals(2, javaList.get(1), "toList second element mismatch");
        assertEquals(3, javaList.get(2), "toList third element mismatch");

        UnsupportedOperationException uoe = assertThrows(UnsupportedOperationException.class,
                () -> javaList.add(4),
                "toList should return an unmodifiable list");
        assertNotNull(uoe, "assertThrows should return the caught exception");
    }

    @Test
    void testIteratorWithLazyEvaluation() {
        // Enqueue forces elements into both the front and rear lists
        PersistentQueue<Integer> queue = PersistentQueue.<Integer>empty()
                .enqueue(10) // goes to front
                .enqueue(20) // goes to rear
                .enqueue(30); // goes to rear

        Iterator<Integer> it = queue.iterator();

        assertTrue(it.hasNext(), "Iterator should have next");
        assertEquals(10, it.next(), "First element should be 10 (from front)");

        assertTrue(it.hasNext(), "Iterator should have next");
        assertEquals(20, it.next(), "Second element should be 20 (lazy eval of rear reversed)");

        assertTrue(it.hasNext(), "Iterator should have next");
        assertEquals(30, it.next(), "Third element should be 30");

        assertFalse(it.hasNext(), "Iterator should be empty");

        NoSuchElementException nsee = assertThrows(NoSuchElementException.class,
                it::next,
                "Iterator should throw on exhaust");
        assertNotNull(nsee, "assertThrows should return the caught exception");
    }

    @Test
    void testEqualsAndHashCode() {
        PersistentQueue<Integer> q1 = PersistentQueue.of(1, 2, 3);
        PersistentQueue<Integer> q2 = PersistentQueue.<Integer>empty().enqueue(1).enqueue(2).enqueue(3);
        PersistentQueue<Integer> q3 = PersistentQueue.of(1, 2, 4);

        assertEquals(q1, q2, "Queues constructed differently but containing the same elements in the same order should be equal");
        assertEquals(q1.hashCode(), q2.hashCode(), "Identical queues should have same hashCode");

        assertNotEquals(q1, q3, "Different queues should not be equal");
        assertNotEquals(null, q1, "Queue should not equal null");
        assertNotEquals("String", q1, "Queue should not equal different type");
    }

    @Test
    void testToString() {
        assertEquals("[]", PersistentQueue.empty().toString(), "Empty toString mismatch");
        assertEquals("[1]", PersistentQueue.of(1).toString(), "Single element toString mismatch");
        assertEquals("[1, 2, 3]", PersistentQueue.of(1, 2, 3).toString(), "Multiple element toString mismatch");
    }

    @Test
    void testSerialization() {
        PersistentQueue<String> original = PersistentQueue.of("Amortized", "O(1)", "Rebalancing", "Serialization");
        PersistentQueue<String> deserialized;

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(original);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            try (ObjectInputStream ois = new ObjectInputStream(bais)) {
                @SuppressWarnings("unchecked")
                PersistentQueue<String> read = (PersistentQueue<String>) ois.readObject();
                deserialized = read;
            }

            assertEquals(original, deserialized, "Deserialized queue does not match original");
            assertEquals(original.dequeue(), deserialized.dequeue(), "Operations on deserialized queue should match");

        } catch (IOException | ClassNotFoundException e) {
            fail("Serialization threw an exception: " + e.getMessage());
        }
    }

    @Test
    void testEquals() {
        PersistentQueue<Integer> q1 = PersistentQueue.of(1, 2, 3);
        PersistentQueue<Integer> q2 = PersistentQueue.of(1, 2, 3);
        PersistentQueue<Integer> q3 = PersistentQueue.of(3, 2, 1);
        PersistentQueue<Integer> q4 = PersistentQueue.of(1, 2);

        assertEquals(q1, q2, "Queues with same elements in same order should be equal");
        assertNotEquals(q1, q3, "Queues with same elements in different order should not be equal");
        assertNotEquals(q1, "String", "Queue should not be equal to different type");
        assertNotEquals(q1, q4, "Queues with different sizes should not be equal");
    }
}
