import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ConsoleUtils Tests")
class ConsoleUtilsTest {

    @Test
    @DisplayName("promptString() should return valid input")
    void testPromptStringValid() {
        String input = "test input\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        String result = ConsoleUtils.promptString(scanner, "Enter text:", true);
        assertEquals("test input", result);
    }

    @Test
    @DisplayName("promptString() should reject empty input when required")
    void testPromptStringEmptyRequired() {
        String input = "\nvalid input\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        String result = ConsoleUtils.promptString(scanner, "Enter text:", true);
        assertEquals("valid input", result);
    }

    @Test
    @DisplayName("promptInt() should return valid integer in range")
    void testPromptIntValid() {
        String input = "5\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        int result = ConsoleUtils.promptInt(scanner, "Enter number (1-10):", 1, 10);
        assertEquals(5, result);
    }

    @Test
    @DisplayName("promptInt() should reject out of range values")
    void testPromptIntOutOfRange() {
        String input = "15\n5\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        int result = ConsoleUtils.promptInt(scanner, "Enter number (1-10):", 1, 10);
        assertEquals(5, result);
    }

    @Test
    @DisplayName("promptInt() should reject non-numeric input")
    void testPromptIntNonNumeric() {
        String input = "abc\n7\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        int result = ConsoleUtils.promptInt(scanner, "Enter number (1-10):", 1, 10);
        assertEquals(7, result);
    }

    @Test
    @DisplayName("promptYesNo() should accept yes/no variants")
    void testPromptYesNo() {
        String input = "yes\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);
        assertTrue(ConsoleUtils.promptYesNo(scanner, "Confirm?"));

        input = "no\n";
        inputStream = new ByteArrayInputStream(input.getBytes());
        scanner = new Scanner(inputStream);
        assertFalse(ConsoleUtils.promptYesNo(scanner, "Confirm?"));
    }

    @Test
    @DisplayName("promptChoice() should return selected option")
    void testPromptChoice() {
        List<String> options = List.of("Option 1", "Option 2", "Option 3");

        String input = "2\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        String result = ConsoleUtils.promptChoice(scanner, "Select option:", options);
        assertEquals("Option 2", result);
    }
}
