import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("User Commands Tests")
class UserCommandsTest {
    private CommandParser parser;
    private RBACSystem system;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        parser = new CommandParser();
        system = new RBACSystem();
        system.initialize();

        system.setCurrentUser("admin");

        CommandRegistry.registerCommands(parser, system);

        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    @DisplayName("user-list should display all users")
    void testUserList() {
        parser.executeCommand("user-list", new Scanner(System.in), system);

        String output = outputStream.toString();
        assertTrue(output.contains("Список пользователей"), "Should show header");
        assertTrue(output.contains("admin"), "Should list admin user");
    }


    @Test
    @DisplayName("user-create should validate email format")
    void testUserCreateInvalidEmail() {
        String input = "testuser\ninvalid user\ninvalid-email\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("user-create", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("Неверный формат email") ||
                        output.contains("Ошибка") ||
                        output.contains("некорректный"),
                "Should show error for invalid email");
        assertFalse(system.getUserManager().exists("testuser"), "User should not be created");
    }

    @Test
    @DisplayName("user-view should display user details")
    void testUserView() {
        var user = User.create("testuser", "Test User", "test@example.com");
        system.getUserManager().add(user);

        String input = "testuser\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("user-view", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("Информация о пользователе"), "Should show header");
        assertTrue(output.contains("testuser"), "Should show username");
        assertTrue(output.contains("Test User"), "Should show full name");
    }

    @Test
    @DisplayName("user-view should handle non-existent user")
    void testUserViewNonExistent() {
        String input = "nonexistent\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("user-view", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("Пользователь не найден"), "Should show error");
    }

    @Test
    @DisplayName("user-update should update user data")
    void testUserUpdate() {
        var user = User.create("testuser", "Test User", "test@example.com");
        system.getUserManager().add(user);

        String input = "testuser\nUpdated User\nupdated@example.com\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("user-update", scanner, system);

        var updatedUser = system.getUserManager().findByUsername("testuser");
        assertTrue(updatedUser.isPresent());
        assertEquals("Updated User", updatedUser.get().fullName());
        assertEquals("updated@example.com", updatedUser.get().email());
    }

    @Test
    @DisplayName("user-delete should delete user")
    void testUserDelete() {
        var user = User.create("testuser", "Test User", "test@example.com");
        system.getUserManager().add(user);

        String input = "testuser\nда\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("user-delete", scanner, system);

        assertFalse(system.getUserManager().exists("testuser"), "User should be deleted");
    }

    @Test
    @DisplayName("user-delete should require confirmation")
    void testUserDeleteConfirmation() {
        var user = User.create("testuser", "Test User", "test@example.com");
        system.getUserManager().add(user);

        String input = "testuser\nнет\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("user-delete", scanner, system);

        assertTrue(system.getUserManager().exists("testuser"), "User should not be deleted without confirmation");
    }

    @Test
    @DisplayName("user-search should filter by username")
    void testUserSearchByUsername() {
        var user1 = User.create("john", "John Doe", "john@example.com");
        var user2 = User.create("jane", "Jane Smith", "jane@example.com");
        system.getUserManager().add(user1);
        system.getUserManager().add(user2);

        String input = "1\njohn\n0\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("user-search", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("john"), "Should find john");
        assertFalse(output.contains("jane"), "Should not find jane");
    }

    @Test
    @DisplayName("user-search should filter by email domain")
    void testUserSearchByEmailDomain() {
        var user1 = User.create("john", "John Doe", "john@company.com");
        var user2 = User.create("jane", "Jane Smith", "jane@example.com");
        system.getUserManager().add(user1);
        system.getUserManager().add(user2);

        String input = "3\n@company.com\n0\n";
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        Scanner scanner = new Scanner(inputStream);

        parser.executeCommand("user-search", scanner, system);

        String output = outputStream.toString();
        assertTrue(output.contains("john"), "Should find john");
        assertFalse(output.contains("jane"), "Should not find jane");
    }
}