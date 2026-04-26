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
@DisplayName("Role Commands Tests")
class RoleCommandsTest {
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
    @DisplayName("role-list should display all roles")
    void testRoleList() {
        parser.executeCommand("role-list", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Список ролей"), "Should show header");
        assertTrue(output.contains("Admin"), "Should list Admin role");
        assertTrue(output.contains("Manager"), "Should list Manager role");
        assertTrue(output.contains("Viewer"), "Should list Viewer role");
    }

    @Test
    @DisplayName("role-create should create new role")
    void testRoleCreate() {
        String input = "TestRole\nTest role description\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("role-create", scanner, system);

        assertTrue(system.getRoleManager().exists("TestRole"), "Role should be created");
    }

    @Test
    @DisplayName("role-create should allow adding permissions")
    void testRoleCreateWithPermissions() {
        String input = "TestRole\nTest role description\nда\ntest\nresource\ndescription\nexit\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("role-create", scanner, system);

        var role = system.getRoleManager().findByName("TestRole");
        assertTrue(role.isPresent(), "Role should be created");
        assertTrue(role.get().hasPermission("test", "resource"), "Role should have permission");
    }

    @Test
    @DisplayName("role-view should display role details")
    void testRoleView() {
        parser.executeCommand("role-view", new Scanner("Admin\n"), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Информация о роли"), "Should show header");
        assertTrue(output.contains("Admin"), "Should show role name");
        assertTrue(output.contains("Права:"), "Should show permissions count");
    }

    @Test
    @DisplayName("role-view should handle non-existent role")
    void testRoleViewNonExistent() {
        parser.executeCommand("role-view", new Scanner("NonExistent\n"), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Роль не найдена"), "Should show error");
    }

    @Test
    @DisplayName("role-delete should delete role")
    void testRoleDelete() {
        var role = new Role("TestRole", "Test description");
        system.getRoleManager().add(role);

        String input = "TestRole\nда\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("role-delete", scanner, system);

        assertFalse(system.getRoleManager().exists("TestRole"), "Role should be deleted");
    }

    @Test
    @DisplayName("role-delete should prevent deletion of assigned roles")
    void testRoleDeleteWithAssignments() {
        var role = new Role("TestRole", "Test description");
        system.getRoleManager().add(role);

        var user = User.create("testuser", "Test User", "test@example.com");
        system.getUserManager().add(user);

        var meta = AssignmentMetadata.now("admin", "Test");
        var assignment = new PermanentAssignment(user, role, meta);
        system.getAssignmentManager().add(assignment);

        String input = "TestRole\nнет\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("role-delete", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("Роль назначена"), "Should warn about assignments");
        assertTrue(output.contains("Удаление отменено"), "Should show cancellation message");
        assertTrue(system.getRoleManager().exists("TestRole"), "Role should NOT be deleted");
    }

    @Test
    @DisplayName("role-add-permission should add permission to role")
    void testRoleAddPermission() {
        var role = new Role("TestRole", "Test description");
        system.getRoleManager().add(role);

        String input = "TestRole\ntestperm\nresource\ndescription\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("role-add-permission", scanner, system);

        var updatedRole = system.getRoleManager().findByName("TestRole");
        assertTrue(updatedRole.isPresent());
        assertTrue(updatedRole.get().hasPermission("testperm", "resource"), "Permission should be added");
    }

    @Test
    @DisplayName("role-remove-permission should remove permission from role")
    void testRoleRemovePermission() {
        var role = new Role("TestRole", "Test description");
        var permission = new Permission("testperm", "resource", "description");
        role.addPermission(permission);
        system.getRoleManager().add(role);

        String input = "TestRole\n1\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("role-remove-permission", scanner, system);

        var updatedRole = system.getRoleManager().findByName("TestRole");
        assertTrue(updatedRole.isPresent());
        assertFalse(updatedRole.get().hasPermission("testperm", "resource"), "Permission should be removed");
    }

    @Test
    @DisplayName("role-search should find roles by name")
    void testRoleSearchByName() {
        String input = "1\nAdmin\n0\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("role-search", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("Admin"), "Should find Admin role");
    }

    @Test
    @DisplayName("role-search should find roles by permission")
    void testRoleSearchByPermission() {
        String input = "2\nREAD\nreports\n0\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("role-search", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("Admin"), "Admin should have READ reports");
    }
}