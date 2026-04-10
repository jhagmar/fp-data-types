package persistent_data_structures;

import org.junit.jupiter.api.Test;
import type_support.Hasher;
import type_support.StandardOrderedHasher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StandardOrderedHasherTest {

    @Test
    void testInitialHashCode() {
        StandardOrderedHasher hasher = new StandardOrderedHasher();
        assertEquals(1, hasher.getHashCode(), "A newly instantiated StandardOrderedHasher must have an initial hash code of 1");
    }

    @Test
    void testSingleElementHash() {
        StandardOrderedHasher hasher = new StandardOrderedHasher();
        String element = "Apple";

        hasher.hash(element);

        // The standard formula: 31 * 1 + element.hashCode()
        int expectedHash = 31 * 1 + element.hashCode();
        assertEquals(expectedHash, hasher.getHashCode(), "Hash code calculation for a single element is incorrect");
    }

    @Test
    void testMultipleElementsMatchesJavaList() {
        StandardOrderedHasher hasher = new StandardOrderedHasher();

        // Use the fluent API to chain hashes
        hasher.hash("A").hash("B").hash("C");

        // Java's built-in List.of() uses the exact same algorithm
        List<String> standardList = List.of("A", "B", "C");

        assertEquals(standardList.hashCode(), hasher.getHashCode(),
                "Accumulated hash code must exactly match the java.util.List#hashCode() implementation");
    }

    @Test
    void testNullRejection() {
        StandardOrderedHasher hasher = new StandardOrderedHasher();

        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> hasher.hash(null),
                "Passing a null element must throw a NullPointerException");

        assertEquals("StandardOrderedHasher does not permit null elements", exception.getMessage(),
                "Exception message must exactly match the defined contract");
    }

    @Test
    void testFluentApiChaining() {
        StandardOrderedHasher hasher = new StandardOrderedHasher();
        Hasher returnedInstance = hasher.hash("Test");

        assertSame(hasher, returnedInstance,
                "The hash() method must return the exact same instance (this) to allow method chaining");
    }

    @Test
    void testDeterministicHashing() {
        StandardOrderedHasher hasher1 = new StandardOrderedHasher();
        hasher1.hash(100).hash(200).hash(300);

        StandardOrderedHasher hasher2 = new StandardOrderedHasher();
        hasher2.hash(100).hash(200).hash(300);

        assertEquals(hasher1.getHashCode(), hasher2.getHashCode(),
                "Hashing the same sequence of objects in two different instances must yield identical results");
    }
}