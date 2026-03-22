import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("FormatUtils Tests")
class FormatUtilsTest {

    @Test
    @DisplayName("formatTable() should generate ASCII table")
    void testFormatTable() {
        String[] headers = {"Username", "Full Name", "Email"};
        List<String[]> rows = List.of(
                new String[]{"admin", "System Administrator", "admin@rbac.system"},
                new String[]{"john", "John Doe", "john@example.com"}
        );

        String table = FormatUtils.formatTable(headers, rows);

        assertTrue(table.contains("Username"), "Should contain header");
        assertTrue(table.contains("Full Name"), "Should contain header");
        assertTrue(table.contains("Email"), "Should contain header");
        assertTrue(table.contains("admin"), "Should contain admin");
        assertTrue(table.contains("john"), "Should contain john");
        assertTrue(table.contains("+"), "Should contain table borders");
        assertTrue(table.contains("|"), "Should contain column separators");
    }

    @Test
    @DisplayName("formatBox() should wrap text in box")
    void testFormatBox() {
        String text = "Test Message";
        String box = FormatUtils.formatBox(text);

        assertTrue(box.contains("Test Message"), "Should contain text");
        assertTrue(box.contains("┌"), "Should contain top-left corner");
        assertTrue(box.contains("┐"), "Should contain top-right corner");
        assertTrue(box.contains("└"), "Should contain bottom-left corner");
        assertTrue(box.contains("┘"), "Should contain bottom-right corner");
    }

    @Test
    @DisplayName("formatHeader() should format section header")
    void testFormatHeader() {
        String header = "User Management";
        String formatted = FormatUtils.formatHeader(header);

        assertTrue(formatted.contains("User Management"), "Should contain header text");
        assertTrue(formatted.contains("="), "Should contain separator");
    }

    @Test
    @DisplayName("truncate() should shorten long strings")
    void testTruncate() {
        String longText = "This is a very long string that needs to be truncated";
        String truncated = FormatUtils.truncate(longText, 20);

        assertEquals("This is a very lo...", truncated);
        assertEquals(20, truncated.length());
    }

    @Test
    @DisplayName("padRight() should add spaces to right")
    void testPadRight() {
        String padded = FormatUtils.padRight("test", 10);
        assertEquals("test      ", padded);
        assertEquals(10, padded.length());
    }

    @Test
    @DisplayName("padLeft() should add spaces to left")
    void testPadLeft() {
        String padded = FormatUtils.padLeft("test", 10);
        assertEquals("      test", padded);
        assertEquals(10, padded.length());
    }
}