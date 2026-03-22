import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("UserSorters Tests")
class UserSortersTest {
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = User.create("charlie", "Charlie Brown", "charlie@example.com");
        user2 = User.create("alice", "Alice Johnson", "alice@example.com");
        user3 = User.create("bob", "Bob Smith", "bob@example.com");
    }

    @Test
    @DisplayName("byUsername() should sort users by username alphabetically")
    void testSortByUsername() {
        List<User> users = Arrays.asList(user1, user2, user3);

        users.sort(UserSorters.byUsername());

        assertEquals("alice", users.get(0).username());
        assertEquals("bob", users.get(1).username());
        assertEquals("charlie", users.get(2).username());
    }

    @Test
    @DisplayName("byFullName() should sort users by full name alphabetically")
    void testSortByFullName() {
        List<User> users = Arrays.asList(user1, user2, user3);

        users.sort(UserSorters.byFullName());

        assertEquals("alice", users.get(0).username());
        assertEquals("bob", users.get(1).username());
        assertEquals("charlie", users.get(2).username());
    }

    @Test
    @DisplayName("byEmail() should sort users by email alphabetically")
    void testSortByEmail() {
        List<User> users = Arrays.asList(user1, user2, user3);

        users.sort(UserSorters.byEmail());

        assertEquals("alice", users.get(0).username());
        assertEquals("bob", users.get(1).username());
        assertEquals("charlie", users.get(2).username());
    }

    @Test
    @DisplayName("Sorters should handle empty lists")
    void testSortEmptyList() {
        List<User> users = new ArrayList<>();

        users.sort(UserSorters.byUsername());
        assertTrue(users.isEmpty());
    }

    @Test
    @DisplayName("Sorters should handle single element lists")
    void testSortSingleElement() {
        List<User> users = new ArrayList<>(List.of(user1));

        users.sort(UserSorters.byUsername());
        assertEquals(1, users.size());
        assertEquals("charlie", users.get(0).username());
    }
}
