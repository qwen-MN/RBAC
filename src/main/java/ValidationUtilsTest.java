import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ValidationUtils Tests")
class ValidationUtilsTest {

    @Test
    @DisplayName("isValidUsername() should accept valid usernames")
    void testValidUsernames() {
        assertTrue(ValidationUtils.isValidUsername("john_doe"));
        assertTrue(ValidationUtils.isValidUsername("jane123"));
        assertTrue(ValidationUtils.isValidUsername("admin"));
        assertTrue(ValidationUtils.isValidUsername("a1_b2_c3"));
    }

    @Test
    @DisplayName("isValidUsername() should reject invalid usernames")
    void testInvalidUsernames() {
        assertFalse(ValidationUtils.isValidUsername(""));
        assertFalse(ValidationUtils.isValidUsername("ab"));
        assertFalse(ValidationUtils.isValidUsername("a".repeat(21)));
        assertFalse(ValidationUtils.isValidUsername("user name"));
        assertFalse(ValidationUtils.isValidUsername("user@name"));
    }

    @Test
    @DisplayName("isValidEmail() should accept valid emails")
    void testValidEmails() {
        assertTrue(ValidationUtils.isValidEmail("user@example.com"));
        assertTrue(ValidationUtils.isValidEmail("john.doe@company.co.uk"));
        assertTrue(ValidationUtils.isValidEmail("admin_123@sub.domain.org"));
    }

    @Test
    @DisplayName("isValidEmail() should reject invalid emails")
    void testInvalidEmails() {
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(ValidationUtils.isValidEmail("@example.com"));
        assertFalse(ValidationUtils.isValidEmail("user@"));
        assertFalse(ValidationUtils.isValidEmail("user@@example.com"));
    }

    @Test
    @DisplayName("isValidDate() should accept valid date formats")
    void testValidDates() {
        assertTrue(ValidationUtils.isValidDate("2026-12-31 23:59"));
        assertTrue(ValidationUtils.isValidDate("2026-01-01 00:00"));
        assertTrue(ValidationUtils.isValidDate("2026-12-31"));
    }

    @Test
    @DisplayName("isValidDate() should reject invalid date formats")
    void testInvalidDates() {
        assertFalse(ValidationUtils.isValidDate("31-12-2026"));
        assertFalse(ValidationUtils.isValidDate("2026/12/31"));
        assertFalse(ValidationUtils.isValidDate("invalid date"));
    }

    @Test
    @DisplayName("normalizeString() should trim and lowercase")
    void testNormalizeString() {
        assertEquals("test", ValidationUtils.normalizeString("  TEST  "));
        assertEquals("hello world", ValidationUtils.normalizeString("  HELLO WORLD  "));
        assertEquals("", ValidationUtils.normalizeString("   "));
    }

    @Test
    @DisplayName("requireNonEmpty() should throw exception for empty values")
    void testRequireNonEmpty() {
        assertDoesNotThrow(() -> ValidationUtils.requireNonEmpty("value", "field"));

        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty("", "field");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty("   ", "field");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ValidationUtils.requireNonEmpty(null, "field");
        });
    }
}
