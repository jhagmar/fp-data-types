package execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AtomicReferenceCompareAndSettable Specification")
public class AtomicReferenceCompareAndSettableTest {

    private static final String INITIAL = "INITIAL_STATE";
    private static final String UPDATED = "UPDATED_STATE";
    private static final String WRONG_EXPECTATION = "WRONG_STATE";

    @Nested
    @DisplayName("Factory Method Tests")
    public class Factory {

        @Test
        @DisplayName("Should initialize correctly with non-null value")
        public void shouldInitializeWithNonNullValue() {
            AtomicReferenceCompareAndSettable<String> wrapper = 
                AtomicReferenceCompareAndSettable.of(INITIAL);
            
            assertEquals(INITIAL, wrapper.get(), "Internal state should match initial value");
        }

        @Test
        @DisplayName("Should throw NullPointerException when initial value is null")
        public void shouldThrowOnNullInitialValue() {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                AtomicReferenceCompareAndSettable.of(null)
            );
            assertEquals("Initial state cannot be null", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("State Management Tests")
    public class StateManagement {

        @Test
        @DisplayName("get() should return the current state")
        public void shouldReturnCurrentState() {
            AtomicReferenceCompareAndSettable<String> wrapper = 
                AtomicReferenceCompareAndSettable.of(INITIAL);
            
            assertEquals(INITIAL, wrapper.get());
        }

        @Test
        @DisplayName("trySet should return true and update value when expectation matches")
        public void trySetSuccess() {
            AtomicReferenceCompareAndSettable<String> wrapper = 
                AtomicReferenceCompareAndSettable.of(INITIAL);

            boolean result = wrapper.trySet(INITIAL, UPDATED);

            assertTrue(result, "CAS should succeed when expectation is correct");
            assertEquals(UPDATED, wrapper.get(), "Value should have been updated");
        }

        @Test
        @DisplayName("trySet should return false and not update value when expectation fails")
        public void trySetFailure() {
            AtomicReferenceCompareAndSettable<String> wrapper = 
                AtomicReferenceCompareAndSettable.of(INITIAL);

            boolean result = wrapper.trySet(WRONG_EXPECTATION, UPDATED);

            assertFalse(result, "CAS should fail when expectation is incorrect");
            assertEquals(INITIAL, wrapper.get(), "Value should remain unchanged");
        }

        @Test
        @DisplayName("trySet should throw NullPointerException if new value is null")
        public void trySetNullValueFail() {
            AtomicReferenceCompareAndSettable<String> wrapper = 
                AtomicReferenceCompareAndSettable.of(INITIAL);

            NullPointerException exception = assertThrows(NullPointerException.class, () -> 
                wrapper.trySet(INITIAL, null)
            );
            
            assertEquals("Cannot swap to a null state", exception.getMessage());
            assertEquals(INITIAL, wrapper.get(), "State should not have changed");
        }

        @Test
        @DisplayName("trySet should handle null expectedValue gracefully (returning false)")
        public void trySetNullExpectedValue() {
            AtomicReferenceCompareAndSettable<String> wrapper = 
                AtomicReferenceCompareAndSettable.of(INITIAL);

            boolean result = wrapper.trySet(null, UPDATED);

            assertFalse(result, "CAS should fail because current state is non-null");
            assertEquals(INITIAL, wrapper.get());
        }
    }
}