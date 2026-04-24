package execution;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@DisplayName("UUIDTraceIdFactory Specification")
public class UUIDTraceIdFactoryTest {

    private UUIDTraceIdFactory factory;

    @BeforeEach
    public void setUp() {
        factory = new UUIDTraceIdFactory();
    }

    @Test
    @DisplayName("Should return a non-null and non-empty string")
    public void createTraceId_ReturnsNonNullValue() {
        String traceId = factory.createTraceId();

        assertNotNull(traceId, "Trace ID should not be null");
        assertFalse(traceId.isEmpty(), "Trace ID should not be empty");
    }

    @Test
    @DisplayName("Should return a valid UUID string format")
    public void createTraceId_ReturnsValidUUIDFormat() {
        String traceId = factory.createTraceId();

        assertDoesNotThrow(() -> UUID.fromString(traceId),
                "The returned Trace ID should be a valid UUID string");
    }

    @RepeatedTest(100)
    @DisplayName("Should generate unique IDs on consecutive calls")
    public void createTraceId_GeneratesUniqueValues() {
        String id1 = factory.createTraceId();
        String id2 = factory.createTraceId();

        assertNotEquals(id1, id2, "Consecutive trace IDs should be unique");
    }

    @Test
    @DisplayName("Bulk uniqueness check")
    public void createTraceId_BulkUniquenessCheck() {
        int count = 1000;
        Set<String> ids = new HashSet<>();

        for (int i = 0; i < count; i++) {
            ids.add(factory.createTraceId());
        }

        assertEquals(count, ids.size(),
                "All generated IDs in a large batch should be unique");
    }
}
