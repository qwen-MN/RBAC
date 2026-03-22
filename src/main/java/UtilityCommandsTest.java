import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Utility Commands Tests")
class UtilityCommandsTest {
    private CommandParser parser;
    private RBACSystem system;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();
        system.initialize();

        CommandRegistry.registerCommands(parser, system);

        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    @DisplayName("help should display all commands")
    void testHelp() {
        parser.executeCommand("help", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Доступные команды"), "Should show header");
        assertTrue(output.contains("user-list"), "Should list user-list command");
        assertTrue(output.contains("role-list"), "Should list role-list command");
        assertTrue(output.contains("assign-role"), "Should list assign-role command");
        assertFalse(output.contains("permissions-check"), "Should list permissions-check command");
        assertTrue(output.contains("stats"), "Should list stats command");
    }

    @Test
    @DisplayName("stats should display system statistics")
    void testStats() {
        parser.executeCommand("stats", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Статистика системы"), "Should show header");
        assertTrue(output.contains("Пользователей: 1"), "Should show user count");
        assertTrue(output.contains("Ролей: 3"), "Should show role count");
        assertTrue(output.contains("Назначений всего: 1"), "Should show assignment count");
        assertFalse(output.contains("Топ-3 ролей:"), "Should show top roles");
    }

    @Test
    @DisplayName("clear should output ANSI escape codes")
    void testClear() {
        parser.executeCommand("clear", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("\033[H\033[2J"), "Should output ANSI clear codes");
    }

    @Test
    @DisplayName("save should save data to file")
    void testSave() {
        String input = "test_save.txt\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("save", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("Данные успешно сохранены"), "Should confirm save");
        assertTrue(output.contains("test_save.txt"), "Should mention filename");
    }

    @Test
    @DisplayName("load should load data from file")
    void testLoad() {
        String saveInput = "test_load.txt\n";
        InputStream saveStream = new ByteArrayInputStream(saveInput.getBytes());
        parser.executeCommand("save", new Scanner(saveStream), system);

        system.getUserManager().clear();
        system.getRoleManager().clear();
        system.getAssignmentManager().clear();

        String loadInput = "test_load.txt\nда\n";
        InputStream loadStream = new ByteArrayInputStream(loadInput.getBytes());
        parser.executeCommand("load", new Scanner(loadStream), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Данные успешно загружены"), "Should confirm load");
        assertTrue(system.getUserManager().exists("admin"), "Should restore admin user");
    }

    @Test
    @DisplayName("load should require confirmation")
    void testLoadConfirmation() {
        String saveInput = "test_confirm.txt\n";
        InputStream saveStream = new ByteArrayInputStream(saveInput.getBytes());
        parser.executeCommand("save", new Scanner(saveStream), system);

        String loadInput = "test_confirm.txt\nнет\n";
        InputStream loadStream = new ByteArrayInputStream(loadInput.getBytes());
        parser.executeCommand("load", new Scanner(loadStream), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Загрузка отменена"), "Should cancel without confirmation");
    }
}
