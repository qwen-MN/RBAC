import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("UserManager Tests")
class UserManagerTest {
    private UserManager userManager;
    private User testUser1;
    private User testUser2;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();

        testUser1 = User.create("john", "John Doe", "john@example.com");
        testUser2 = User.create("jane", "Jane Smith", "jane@example.com");
    }

    @Test
    @DisplayName("add() should add user successfully")
    void testAddUser() {
        userManager.add(testUser1);

        assertTrue(userManager.exists("john"));
        assertEquals(1, userManager.count());
    }

    @Test
    @DisplayName("add() should throw exception for duplicate username")
    void testAddDuplicateUser() {
        userManager.add(testUser1);

        User duplicate = User.create("john", "John Smith", "john.smith@example.com");

        assertThrows(IllegalArgumentException.class, () -> {
            userManager.add(duplicate);
        });

        assertEquals(1, userManager.count());
    }

    @Test
    @DisplayName("add() should validate username format")
    void testAddInvalidUsername() {
        assertThrows(IllegalArgumentException.class, () -> {
            User.create("", "Test", "test@example.com");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            User.create("user with spaces", "Test", "test@example.com");
        });
    }

    @Test
    @DisplayName("add() should validate email format")
    void testAddInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> {
            User.create("testuser", "Test", "invalid-email");
        });
    }

    @Test
    @DisplayName("remove() should remove user successfully")
    void testRemoveUser() {
        userManager.add(testUser1);
        userManager.add(testUser2);

        boolean removed = userManager.remove(testUser1);

        assertTrue(removed);
        assertFalse(userManager.exists("john"));
        assertEquals(1, userManager.count());
    }

    @Test
    @DisplayName("remove() should return false for non-existent user")
    void testRemoveNonExistentUser() {
        boolean removed = userManager.remove(testUser1);

        assertFalse(removed);
        assertEquals(0, userManager.count());
    }

    @Test
    @DisplayName("findByUsername() should return user when exists")
    void testFindByUsername() {
        userManager.add(testUser1);

        Optional<User> found = userManager.findByUsername("john");

        assertTrue(found.isPresent());
        assertEquals("John Doe", found.get().fullName());
        assertEquals("john@example.com", found.get().email());
    }

    @Test
    @DisplayName("findByUsername() should return empty when user not exists")
    void testFindByUsernameNotFound() {
        Optional<User> found = userManager.findByUsername("unknown");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("findByEmail() should return user when email exists")
    void testFindByEmail() {
        userManager.add(testUser1);

        Optional<User> found = userManager.findByEmail("john@example.com");

        assertTrue(found.isPresent());
        assertEquals("john", found.get().username());
    }

    @Test
    @DisplayName("findByEmail() should return empty when email not exists")
    void testFindByEmailNotFound() {
        Optional<User> found = userManager.findByEmail("unknown@example.com");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("update() should update user data successfully")
    void testUpdateUser() {
        userManager.add(testUser1);

        userManager.update("john", "John Updated", "john.updated@example.com");

        Optional<User> updated = userManager.findByUsername("john");
        assertTrue(updated.isPresent());
        assertEquals("John Updated", updated.get().fullName());
        assertEquals("john.updated@example.com", updated.get().email());
    }

    @Test
    @DisplayName("update() should throw exception for non-existent user")
    void testUpdateNonExistentUser() {
        assertThrows(NoSuchElementException.class, () -> {
            userManager.update("unknown", "Test", "test@example.com");
        });
    }

    @Test
    @DisplayName("update() should throw exception for invalid email")
    void testUpdateWithInvalidEmail() {
        userManager.add(testUser1);

        assertThrows(IllegalArgumentException.class, () -> {
            userManager.update("john", "John", "invalid-email");
        });
    }

    @Test
    @DisplayName("findByFilter() should filter users by email domain")
    void testFindByFilter() {
        userManager.add(testUser1);
        userManager.add(testUser2);
        userManager.add(User.create("bob", "Bob Johnson", "bob@company.com"));

        List<User> filtered = userManager.findByFilter(
                UserFilters.byEmailDomain("@example.com")
        );

        assertEquals(2, filtered.size());
        assertTrue(filtered.stream().anyMatch(u -> u.username().equals("john")));
        assertTrue(filtered.stream().anyMatch(u -> u.username().equals("jane")));
    }

    @Test
    @DisplayName("findAll() with filter and sorter should work correctly")
    void testFindAllWithFilterAndSorter() {
        userManager.add(User.create("charlie", "Charlie Brown", "charlie@example.com"));
        userManager.add(testUser2);
        userManager.add(testUser1);

        List<User> result = userManager.findAll(
                UserFilters.byEmailDomain("@example.com"),
                UserSorters.byUsername()
        );

        assertEquals(3, result.size());
        assertEquals("charlie", result.get(0).username());
        assertEquals("jane", result.get(1).username());
        assertEquals("john", result.get(2).username());
    }

    @Test
    @DisplayName("findAll() without filter should return all users")
    void testFindAllWithoutFilter() {
        userManager.add(testUser1);
        userManager.add(testUser2);

        List<User> all = userManager.findAll(null, null);

        assertEquals(2, all.size());
        assertTrue(all.contains(testUser1));
        assertTrue(all.contains(testUser2));
    }

    @Test
    @DisplayName("clear() should remove all users")
    void testClear() {
        userManager.add(testUser1);
        userManager.add(testUser2);

        assertEquals(2, userManager.count());

        userManager.clear();

        assertEquals(0, userManager.count());
        assertTrue(userManager.findAll().isEmpty());
    }

    @Test
    @DisplayName("equals() and hashCode() should work correctly")
    void testEqualsAndHashCode() {
        UserManager manager1 = new UserManager();
        UserManager manager2 = new UserManager();

        manager1.add(testUser1);
        manager2.add(testUser1);

        assertEquals(manager1, manager2);
        assertEquals(manager1.hashCode(), manager2.hashCode());

        manager1.add(testUser2);

        assertNotEquals(manager1, manager2);
    }

    @Test
    @DisplayName("Repository methods should be implemented correctly")
    void testRepositoryMethods() {
        userManager.add(testUser1);
        assertEquals(1, userManager.count());

        Optional<User> found = userManager.findById("john");
        assertTrue(found.isPresent());

        List<User> all = userManager.findAll();
        assertEquals(1, all.size());

        boolean removed = userManager.remove(testUser1);
        assertTrue(removed);
        assertEquals(0, userManager.count());

        userManager.add(testUser1);
        userManager.add(testUser2);
        userManager.clear();
        assertEquals(0, userManager.count());
    }
}