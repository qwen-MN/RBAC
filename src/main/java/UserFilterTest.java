import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("UserFilter Tests")
class UserFilterTest {
    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        user1 = User.create("john", "John Doe", "john@example.com");
        user2 = User.create("jane", "Jane Smith", "jane@company.com");
        user3 = User.create("bob", "Bob Johnson", "bob@example.com");
    }

    @Test
    @DisplayName("byUsername() should match exact username")
    void testByUsernameExactMatch() {
        UserFilter filter = UserFilters.byUsername("john");
        assertTrue(filter.test(user1));
        assertFalse(filter.test(user2));
        assertFalse(filter.test(user3));
    }

    @Test
    @DisplayName("byUsername() should not match different case")
    void testByUsernameCaseSensitive() {
        UserFilter filter = UserFilters.byUsername("JOHN");
        assertFalse(filter.test(user1));
    }

    @Test
    @DisplayName("byUsernameContains() should match substring case-insensitively")
    void testByUsernameContains() {
        UserFilter filter = UserFilters.byUsernameContains("JO");
        assertTrue(filter.test(user1));
        assertFalse(filter.test(user2));
        assertFalse(filter.test(user3));
    }

    @Test
    @DisplayName("byUsernameContains() should handle empty substring")
    void testByUsernameContainsEmpty() {
        UserFilter filter = UserFilters.byUsernameContains("");
        assertTrue(filter.test(user1));
        assertTrue(filter.test(user2));
        assertTrue(filter.test(user3));
    }

    @Test
    @DisplayName("byEmail() should match exact email")
    void testByEmailExactMatch() {
        UserFilter filter = UserFilters.byEmail("john@example.com");
        assertTrue(filter.test(user1));
        assertFalse(filter.test(user2));
        assertFalse(filter.test(user3));
    }

    @Test
    @DisplayName("byEmailDomain() should match email ending with domain")
    void testByEmailDomain() {
        UserFilter filter = UserFilters.byEmailDomain("@example.com");
        assertTrue(filter.test(user1));
        assertFalse(filter.test(user2));
        assertTrue(filter.test(user3));
    }

    @Test
    @DisplayName("byEmailDomain() should handle domain without @ prefix")
    void testByEmailDomainWithoutAt() {
        UserFilter filter = UserFilters.byEmailDomain("example.com");
        assertTrue(filter.test(user1));
    }

    @Test
    @DisplayName("byFullNameContains() should match substring in full name case-insensitively")
    void testByFullNameContains() {
        UserFilter filter = UserFilters.byFullNameContains("john");
        assertTrue(filter.test(user1));
        assertFalse(filter.test(user2));
        assertTrue(filter.test(user3));
    }

    @Test
    @DisplayName("and() should combine filters with logical AND")
    void testAndCombination() {
        UserFilter byDomain = UserFilters.byEmailDomain("@example.com");
        UserFilter byName = UserFilters.byFullNameContains("John");

        UserFilter combined = byDomain.and(byName);

        assertTrue(combined.test(user1));
        assertFalse(combined.test(user2));
        assertTrue(combined.test(user3));
    }

    @Test
    @DisplayName("or() should combine filters with logical OR")
    void testOrCombination() {
        UserFilter byDomain = UserFilters.byEmailDomain("@example.com");
        UserFilter byName = UserFilters.byFullNameContains("Smith");

        UserFilter combined = byDomain.or(byName);

        assertTrue(combined.test(user1));
        assertTrue(combined.test(user2));
        assertTrue(combined.test(user3));
    }

    @Test
    @DisplayName("Filter should work with stream operations")
    void testFilterWithStream() {
        List<User> users = Arrays.asList(user1, user2, user3);

        List<User> filtered = users.stream()
                .filter(UserFilters.byEmailDomain("@example.com")::test)
                .toList();

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(user1));
        assertTrue(filtered.contains(user3));
    }

    @Test
    @DisplayName("Filter should handle null user gracefully")
    void testFilterWithNullUser() {
        UserFilter filter = UserFilters.byUsername("test");
        assertThrows(NullPointerException.class, () -> filter.test(null));
    }
}
