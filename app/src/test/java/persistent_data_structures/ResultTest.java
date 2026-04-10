package persistent_data_structures;

import fp_types.Result;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Full-coverage test suite for the {@link Result} discriminated union type.
 */
class ResultTest {

    private final Result<String, Integer> okResult = Result.ok("Success");
    private final Result<String, Integer> errResult = Result.err(404);

    @Test
    void ok_WithNonNullValue_CreatesOkInstance() {
        // Arrange & Act
        Result<String, Integer> result = Result.ok("Success");

        // Assert
        assertTrue(result.isOk());
        assertFalse(result.isErr());
        assertEquals("Success", result.unwrap());
    }

    @Test
    void ok_WithNullValue_ThrowsNullPointerException() {
        // Arrange, Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> Result.ok(null));
        assertEquals("Success value cannot be null", exception.getMessage());
    }

    @Test
    void err_WithNonNullError_CreatesErrInstance() {
        // Arrange & Act
        Result<String, Integer> result = Result.err(404);

        // Assert
        assertTrue(result.isErr());
        assertFalse(result.isOk());
        assertEquals(404, result.unwrapErr());
    }

    @Test
    void err_WithNullError_ThrowsNullPointerException() {
        // Arrange, Act & Assert
        NullPointerException exception = assertThrows(NullPointerException.class, () -> Result.err(null));
        assertEquals("Error value cannot be null", exception.getMessage());
    }

    @Test
    void isOk_ReturnsTrue() {
        assertTrue(okResult.isOk());
    }

    @Test
    void isErr_ReturnsFalse() {
        assertFalse(okResult.isErr());
    }

    @Test
    void unwrap_ReturnsValue() {
        assertEquals("Success", okResult.unwrap());
    }

    @Test
    void unwrapErr_ThrowsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, okResult::unwrapErr);
        assertTrue(exception.getMessage().contains("Called unwrapErr() on an Ok value"));
    }

    @Test
    void unwrapOr_ReturnsUnderlyingValue() {
        assertEquals("Success", okResult.unwrapOr("Default"));
    }

    @Test
    void unwrapOrElse_DoesNotInvokeFallbackAndReturnsValue() {
        assertEquals("Success", okResult.unwrapOrElse(err -> "Fallback: " + err));
    }

    @Test
    void unwrapOrElseThrow_DoesNotThrowAndReturnsValue() {
        assertDoesNotThrow(() -> {
            String value = okResult.unwrapOrElseThrow(err -> new IllegalArgumentException("Error: " + err));
            assertEquals("Success", value);
        });
    }

    @Test
    void map_TransformsValue() {
        // Act
        Result<Integer, Integer> mapped = okResult.map(String::length);

        // Assert
        assertTrue(mapped.isOk());
        assertEquals(7, mapped.unwrap());
    }

    @Test
    void mapErr_IgnoresTransformationAndMaintainsValue() {
        // Act
        Result<String, String> mapped = okResult.mapErr(err -> "Error: " + err);

        // Assert
        assertTrue(mapped.isOk());
        assertEquals("Success", mapped.unwrap());
    }

    @Test
    void flatMap_ReturnsNewResult() {
        // Act
        Result<String, Integer> flatMapped = okResult.flatMap(val -> Result.ok(val + " Updated"));

        // Assert
        assertEquals("Success Updated", flatMapped.unwrap());
    }

    @Test
    void flatMapErr_IgnoresTransformation() {
        // Act
        Result<String, String> flatMapped = okResult.flatMapErr(err -> Result.err("New Error"));

        // Assert
        assertTrue(flatMapped.isOk());
        assertEquals("Success", flatMapped.unwrap());
    }

    @Test
    void match_AppliesOkFunction() {
        // Act
        String outcome = okResult.match(
                val -> "Matched Value: " + val,
                err -> "Matched Error: " + err
        );

        // Assert
        assertEquals("Matched Value: Success", outcome);
    }

    @Test
    void ifOk_ExecutesConsumer() {
        // Arrange
        AtomicBoolean wasExecuted = new AtomicBoolean(false);

        // Act
        okResult.ifOk(val -> wasExecuted.set(true));

        // Assert
        assertTrue(wasExecuted.get(), "Consumer should have been executed for Ok");
    }

    @Test
    void ifErr_IgnoresConsumer() {
        // Arrange
        AtomicBoolean wasExecuted = new AtomicBoolean(false);

        // Act
        okResult.ifErr(err -> wasExecuted.set(true));

        // Assert
        assertFalse(wasExecuted.get(), "Consumer should NOT have been executed for Ok");
    }

    @Test
    void toOptional_ReturnsPresentOptional() {
        // Act
        Optional<String> optional = okResult.toOptional();

        // Assert
        assertTrue(optional.isPresent());
        assertEquals("Success", optional.get());
    }

    @Test
    void isOk_ReturnsFalse() {
        assertFalse(errResult.isOk());
    }

    @Test
    void isErr_ReturnsTrue() {
        assertTrue(errResult.isErr());
    }

    @Test
    void unwrap_ThrowsIllegalStateException() {
        IllegalStateException exception = assertThrows(IllegalStateException.class, errResult::unwrap);
        assertTrue(exception.getMessage().contains("Called unwrap() on an Err value"));
    }

    @Test
    void unwrapErr_ReturnsErrorValue() {
        assertEquals(404, errResult.unwrapErr());
    }

    @Test
    void unwrapOr_ReturnsProvidedDefault() {
        assertEquals("Default", errResult.unwrapOr("Default"));
    }

    @Test
    void unwrapOrElse_AppliesFallbackFunction() {
        // Act
        String result = errResult.unwrapOrElse(err -> "Error Code: " + err);

        // Assert
        assertEquals("Error Code: 404", result);
    }

    @Test
    void unwrapOrElseThrow_ThrowsProvidedException() {
        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> errResult.unwrapOrElseThrow(err -> new IllegalArgumentException("Failed with: " + err))
        );
        assertEquals("Failed with: 404", exception.getMessage());
    }

    @Test
    void map_IgnoresTransformationAndMaintainsError() {
        // Act
        Result<Integer, Integer> mapped = errResult.map(String::length);

        // Assert
        assertTrue(mapped.isErr());
        assertEquals(404, mapped.unwrapErr());
    }

    @Test
    void mapErr_TransformsErrorValue() {
        // Act
        Result<String, String> mapped = errResult.mapErr(err -> "HTTP " + err);

        // Assert
        assertTrue(mapped.isErr());
        assertEquals("HTTP 404", mapped.unwrapErr());
    }

    @Test
    void flatMap_IgnoresTransformation() {
        // Act
        Result<Integer, Integer> flatMapped = errResult.flatMap(val -> Result.ok(val.length()));

        // Assert
        assertTrue(flatMapped.isErr());
        assertEquals(404, flatMapped.unwrapErr());
    }

    @Test
    void flatMapErr_ReturnsNewResult() {
        // Act
        Result<String, String> flatMapped = errResult.flatMapErr(err -> Result.err("Fatal: " + err));

        // Assert
        assertTrue(flatMapped.isErr());
        assertEquals("Fatal: 404", flatMapped.unwrapErr());
    }

    @Test
    void match_AppliesErrFunction() {
        // Act
        String outcome = errResult.match(
                val -> "Matched Value: " + val,
                err -> "Matched Error: " + err
        );

        // Assert
        assertEquals("Matched Error: 404", outcome);
    }

    @Test
    void ifOk_IgnoresConsumer() {
        // Arrange
        AtomicBoolean wasExecuted = new AtomicBoolean(false);

        // Act
        errResult.ifOk(val -> wasExecuted.set(true));

        // Assert
        assertFalse(wasExecuted.get(), "Consumer should NOT have been executed for Err");
    }

    @Test
    void ifErr_ExecutesConsumer() {
        // Arrange
        AtomicBoolean wasExecuted = new AtomicBoolean(false);

        // Act
        errResult.ifErr(err -> wasExecuted.set(true));

        // Assert
        assertTrue(wasExecuted.get(), "Consumer should have been executed for Err");
    }

    @Test
    void toOptional_ReturnsEmptyOptional() {
        // Act
        Optional<String> optional = errResult.toOptional();

        // Assert
        assertTrue(optional.isEmpty());
    }
}
