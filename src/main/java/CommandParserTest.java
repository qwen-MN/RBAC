import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CommandParser Tests")
class CommandParserTest {
    private CommandParser parser;
    private RBACSystem system;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();
        system.initialize();

        parser.registerCommand("test", "Test command",
                (scanner, sys) -> System.out.println("Test command executed"));

        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    @DisplayName("registerCommand() should register command successfully")
    void testRegisterCommand() {
        parser.registerCommand("newcommand", "New command",
                (scanner, sys) -> System.out.println("New command executed"));

        parser.executeCommand("newcommand", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("New command executed"), "Command should be executed");
    }

    @Test
    @DisplayName("executeCommand() should execute registered command")
    void testExecuteCommand() {
        parser.executeCommand("test", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Test command executed"), "Command should be executed");
    }

    @Test
    @DisplayName("executeCommand() should handle unknown command")
    void testExecuteUnknownCommand() {
        outputStream.reset();

        parser.executeCommand("unknown", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Команда не найдена"), "Should show error for unknown command");
    }

    @Test
    @DisplayName("parseAndExecute() should parse command from input string")
    void testParseAndExecute() {
        parser.parseAndExecute("test", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Test command executed"), "Command should be parsed and executed");
    }

    @Test
    @DisplayName("parseAndExecute() should handle empty input")
    void testParseAndExecuteEmptyInput() {
        assertDoesNotThrow(() -> {
            parser.parseAndExecute("", new Scanner(System.in), system);
            parser.parseAndExecute("   ", new Scanner(System.in), system);
        });
    }

    @Test
    @DisplayName("printHelp() should print all registered commands")
    void testPrintHelp() {
        parser.printHelp();

        String output = outputStream.toString();
        assertTrue(output.contains("Доступные команды"), "Should contain header");
        assertTrue(output.contains("test"), "Should list test command");
        assertTrue(output.contains("Test command"), "Should show command description");
    }

    @Test
    @DisplayName("Commands should be case-insensitive")
    void testCommandsCaseInsensitive() {
        parser.parseAndExecute("TEST", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Test command executed"), "Commands should be case-insensitive");
    }
}