import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Assignment Commands Tests")
class AssignmentCommandsTest {
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
    @DisplayName("assign-role should create permanent assignment")
    void testAssignRolePermanent() {
        var user = User.create("testuser", "Test User", "test@example.com");
        system.getUserManager().add(user);

        String input = "testuser\n1\n1\nTest reason\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("assign-role", scanner, system);

        assertTrue(system.getAssignmentManager().userHasRole(user,
                system.getRoleManager().findByName("Admin").get()), "User should have Admin role");
    }

    @Test
    @DisplayName("assign-role should create temporary assignment")
    void testAssignRoleTemporary() {
        var user = User.create("testuser", "Test User", "test@example.com");
        system.getUserManager().add(user);

        String futureDate = "2026-12-31 23:59";

        String ls = System.lineSeparator();
        String input = String.join(ls,
                "testuser",
                "1",
                "2",
                futureDate,
                "нет",
                "Test reason"
        ) + ls;

        InputStream inputStream = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8);

        parser.executeCommand("assign-role", scanner, system);

        assertTrue(system.getAssignmentManager().userHasRole(user,
                        system.getRoleManager().findByName("Admin").get()),
                "User should have Admin role");
    }

    @Test
    @DisplayName("revoke-role should revoke permanent assignment")
    void testRevokeRole() {
        var user = User.create("testuser", "Test User", "test@example.com");
        system.getUserManager().add(user);

        var role = system.getRoleManager().findByName("Admin").get();
        var meta = AssignmentMetadata.now("admin", "Test");
        var assignment = new PermanentAssignment(user, role, meta);
        system.getAssignmentManager().add(assignment);

        String input = "testuser\n1\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("revoke-role", scanner, system);

        assertFalse(system.getAssignmentManager().userHasRole(user, role),
                "User should not have role after revoke");
    }

    @Test
    @DisplayName("assignment-list should display all assignments")
    void testAssignmentList() {
        parser.executeCommand("assignment-list", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Все назначения"), "Should show header");
        assertTrue(output.contains("admin"), "Should list admin assignment");
        assertTrue(output.contains("Admin"), "Should list Admin role");
    }

    @Test
    @DisplayName("assignment-active should display active assignments")
    void testAssignmentActive() {
        parser.executeCommand("assignment-active", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Активные назначения"), "Should show header");
        assertTrue(output.contains("admin"), "Should list admin assignment");
    }

    @Test
    @DisplayName("assignment-search should filter by user")
    void testAssignmentSearchByUser() {
        String input = "1\nadmin\n0\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("assignment-search", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("Результаты поиска"), "Should show results");
        assertTrue(output.contains("admin"), "Should find admin assignments");
    }

    @Test
    @DisplayName("assignment-search should filter by role")
    void testAssignmentSearchByRole() {
        String input = "2\nAdmin\n0\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("assignment-search", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("admin"), "Should find admin assignments");
    }
}
