package persistent_data_structures;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PersistentPriorityQueueTest {

    @Test
    void testEmptyQueue() {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.empty();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
        assertEquals(Optional.empty(), queue.peek());
    }

    @Test
    void testAddAndPeek() {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.<Integer>empty()
                .add(5)
                .add(2)
                .add(8)
                .add(1);

        assertFalse(queue.isEmpty());
        assertEquals(4, queue.size());
        assertEquals(Optional.of(1), queue.peek(), "Peek should return the minimum element");
    }

    @Test
    void testAddImmutability() {
        PersistentPriorityQueue<Integer> q1 = PersistentPriorityQueue.empty();
        PersistentPriorityQueue<Integer> q2 = q1.add(5);
        PersistentPriorityQueue<Integer> q3 = q2.add(2);

        assertTrue(q1.isEmpty());

        assertEquals(1, q2.size());
        assertEquals(Optional.of(5), q2.peek());

        assertEquals(2, q3.size());
        assertEquals(Optional.of(2), q3.peek());
    }

    @Test
    void testRemoveMin() {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.<Integer>empty()
                .add(10)
                .add(5)
                .add(20)
                .add(2);

        PersistentPriorityQueue<Integer> q1 = queue.removeMin(); // Removes 2
        assertEquals(3, q1.size());
        assertEquals(Optional.of(5), q1.peek());

        PersistentPriorityQueue<Integer> q2 = q1.removeMin(); // Removes 5
        assertEquals(2, q2.size());
        assertEquals(Optional.of(10), q2.peek());

        PersistentPriorityQueue<Integer> q3 = q2.removeMin(); // Removes 10
        assertEquals(1, q3.size());
        assertEquals(Optional.of(20), q3.peek());

        PersistentPriorityQueue<Integer> q4 = q3.removeMin(); // Removes 20
        assertTrue(q4.isEmpty());
    }

    @Test
    void testRemoveMinFromEmptyReturnsSelf() {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.empty();
        PersistentPriorityQueue<Integer> result = queue.removeMin();

        assertSame(queue, result, "Removing from an empty queue should return the same empty instance");
    }

    @Test
    void testNullElementsRejected() {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.empty();

        NullPointerException exception = assertThrows(NullPointerException.class, () -> queue.add(null));
        assertEquals("PersistentPriorityQueue does not permit null elements", exception.getMessage());
    }

    @Test
    void testMergeNullRejected() {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.<Integer>empty().add(1);

        NullPointerException exception = assertThrows(NullPointerException.class, () -> queue.merge(null));
        assertEquals("Cannot merge with a null queue", exception.getMessage());
    }

    @Test
    void testMergeTwoQueues() {
        PersistentPriorityQueue<Integer> q1 = PersistentPriorityQueue.<Integer>empty().add(5).add(1).add(9);
        PersistentPriorityQueue<Integer> q2 = PersistentPriorityQueue.<Integer>empty().add(3).add(7).add(2);

        PersistentPriorityQueue<Integer> merged = q1.merge(q2);

        assertEquals(6, merged.size());

        // Verify all elements are present and in correct order by successively removing mins
        List<Integer> extracted = new ArrayList<>();
        PersistentPriorityQueue<Integer> current = merged;
        while (!current.isEmpty()) {
            extracted.add(current.peek().get());
            current = current.removeMin();
        }

        assertEquals(Arrays.asList(1, 2, 3, 5, 7, 9), extracted);
    }

    @Test
    void testMergeWithEmptyReturnsSelf() {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.<Integer>empty().add(1).add(2);
        PersistentPriorityQueue<Integer> emptyQueue = PersistentPriorityQueue.empty();

        assertSame(queue, queue.merge(emptyQueue), "Merging with empty should return the original queue");
        assertSame(queue, emptyQueue.merge(queue), "Merging empty with a queue should return the original queue");
    }

    @Test
    void testIteratorExhaustive() {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.<Integer>empty()
                .add(4).add(2).add(6).add(1);

        List<Integer> elements = new ArrayList<>();
        Iterator<Integer> it = queue.iterator();

        while (it.hasNext()) {
            elements.add(it.next());
        }

        assertEquals(4, elements.size());
        assertTrue(elements.containsAll(Arrays.asList(1, 2, 4, 6)));

        NoSuchElementException exception = assertThrows(NoSuchElementException.class, it::next);
        assertNotNull(exception, "Calling next() on an exhausted iterator should throw NoSuchElementException");
    }

    @Test
    void testEqualsAndHashCode() {
        // Insert elements in different orders to generate potentially different Leftist Heap structures
        PersistentPriorityQueue<Integer> q1 = PersistentPriorityQueue.<Integer>empty().add(1).add(2).add(3);
        PersistentPriorityQueue<Integer> q2 = PersistentPriorityQueue.<Integer>empty().add(3).add(2).add(1);
        PersistentPriorityQueue<Integer> q3 = PersistentPriorityQueue.<Integer>empty().add(1).add(4).add(3);
        PersistentPriorityQueue<Integer> q4 = PersistentPriorityQueue.<Integer>empty().add(1).add(2);

        assertEquals(q1, q1); // self
        assertEquals(q1, q2); // same elements, different insertion order
        assertNotEquals(q1, q3); // different elements
        assertNotEquals(q1, q4); // different sizes
        assertNotEquals(null, q1);
        assertNotEquals(new Object(), q1);

        assertEquals(q1.hashCode(), q2.hashCode(), "Equal queues must have identical hash codes");
    }

    @Test
    void testToString() {
        PersistentPriorityQueue<Integer> emptyQueue = PersistentPriorityQueue.empty();
        assertEquals("[]", emptyQueue.toString());

        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.<Integer>empty()
                .add(5).add(1).add(3);

        // toString implementation sorts the elements for predictable output
        assertEquals("[1, 3, 5]", queue.toString());
    }

    @Test
    void testSerializationRoundTrip() throws Exception {
        PersistentPriorityQueue<Integer> original = PersistentPriorityQueue.<Integer>empty()
                .add(10)
                .add(5)
                .add(20);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        PersistentPriorityQueue<Integer> deserialized;
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            @SuppressWarnings("unchecked")
            PersistentPriorityQueue<Integer> casted = (PersistentPriorityQueue<Integer>) ois.readObject();
            deserialized = casted;
        }

        assertNotSame(original, deserialized);
        assertEquals(original, deserialized);
        assertEquals(original.size(), deserialized.size());
        assertEquals(original.peek(), deserialized.peek());
    }

    @Test
    void testDirectReadObjectThrowsException() throws Exception {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.empty();

        // Use reflection to bypass visibility and invoke readObject directly to guarantee 100% line coverage
        Method readObjectMethod = PersistentPriorityQueue.class.getDeclaredMethod("readObject", ObjectInputStream.class);
        readObjectMethod.setAccessible(true);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> readObjectMethod.invoke(queue, (ObjectInputStream) null)
        );

        assertInstanceOf(InvalidObjectException.class, exception.getCause());
        assertEquals("Serialization proxy required", exception.getCause().getMessage());
    }

    @Test
    void testIteratorOnEmptyQueue() {
        PersistentPriorityQueue<Integer> emptyQueue = PersistentPriorityQueue.empty();
        Iterator<Integer> it = emptyQueue.iterator();

        // This hits the branch where `root == null` evaluates to true (skipping the stack push)
        assertFalse(it.hasNext(), "Empty queue iterator should not have a next element");

        // Ensure the empty state respects the Iterator contract
        assertThrows(NoSuchElementException.class, it::next,
                "Calling next() on an empty iterator must throw NoSuchElementException");
    }

    @Test
    void testEqualsDifferentTypeAndNull() {
        PersistentPriorityQueue<Integer> queue = PersistentPriorityQueue.<Integer>empty().add(1);

        // This explicitly forces `!(o instanceof PersistentPriorityQueue<?> that)` to evaluate to true
        assertFalse(queue.equals(new Object()), "Queue should not be equal to a plain Object");
        assertFalse(queue.equals("String literal"), "Queue should not be equal to a String");

        // As a bonus, this hits the implicit null check inside the Java 16+ pattern-matching instanceof operator
        assertFalse(queue.equals(null), "Queue should not be equal to null");
    }
}