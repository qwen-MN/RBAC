import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
@DisplayName("Permission Commands Tests")
class PermissionCommandsTest {
    private CommandParser parser;
    private RBACSystem system;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();
        system.initialize();
        CommandRegistry.registerCommands(parser, system);

        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        outputStream.reset();
    }

    @Test
    @DisplayName("permissions-user should display all user permissions")
    void testPermissionsUser() {
        executeCommand("permissions-user", "admin");

        String output = outputStream.toString();
        assertTrue(output.contains("admin"), "Should show user name");
        assertTrue(output.contains("READ"), "Should show READ permission");
        assertTrue(output.contains("WRITE"), "Should show WRITE permission");
        assertTrue(output.contains("DELETE"), "Should show DELETE permission");
        assertTrue(output.contains("users"), "Should show users");
        assertTrue(output.contains("roles"), "Should show roles");
        assertTrue(output.contains("reports"), "Should show reports");
        assertTrue(output.contains("dashboards"), "Should show dashboards");
    }

    @Test
    @DisplayName("permissions-user should group permissions by resource")
    void testPermissionsUserGroupedByResource() {
        executeCommand("permissions-user", "admin");

        String output = outputStream.toString();
        assertTrue(output.contains("Ресурс:"), "Should show resource grouping");
        assertTrue(output.contains("reports"), "Should show reports resource");
        assertTrue(output.contains("dashboards"), "Should show dashboards resource");
    }

    @Test
    @DisplayName("permissions-check should verify user has permission")
    void testPermissionsCheckHasPermission() {
        executeCommand("permissions-check", "admin", "READ", "reports");

        String output = outputStream.toString();
        assertTrue(output.contains("Да"), "Should confirm permission");
        assertTrue(output.contains("Admin"), "Should show Admin role");
    }

    @Test
    @DisplayName("permissions-check should verify user does not have permission")
    void testPermissionsCheckNoPermission() {
        executeCommand("permissions-check", "admin", "EXECUTE", "reports");

        String output = outputStream.toString();
        assertTrue(output.contains("Нет"), "Should deny permission");
    }

    @Test
    @DisplayName("permissions-check should handle non-existent user")
    void testPermissionsCheckNonExistentUser() {
        executeCommand("permissions-check", "nonexistent", "READ", "reports");

        String output = outputStream.toString();
        assertTrue(output.contains("не найден"), "Should show user not found");
    }

    private void executeCommand(String command, String... inputs) {
        String input = String.join("\n", inputs) + "\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);
        parser.executeCommand(command, scanner, system);
    }
}

