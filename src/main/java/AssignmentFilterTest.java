import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("AssignmentFilter Tests")
class AssignmentFilterTest {
    private User user1;
    private User user2;
    private Role role1;
    private Role role2;
    private RoleAssignment permanentAssignment;
    private RoleAssignment temporaryAssignment;
    private RoleAssignment inactiveAssignment;

    @BeforeEach
    void setUp() {
        user1 = User.create("john", "John Doe", "john@example.com");
        user2 = User.create("jane", "Jane Smith", "jane@example.com");

        role1 = new Role("Admin", "Full access");
        role2 = new Role("Editor", "Edit content");

        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Permanent assignment");
        permanentAssignment = new PermanentAssignment(user1, role1, meta1);

        String futureDate = LocalDateTime.now().plusDays(30)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Temporary assignment");
        temporaryAssignment = new TemporaryAssignment(user2, role2, meta2, futureDate, false);

        AssignmentMetadata meta3 = AssignmentMetadata.now("admin", "Inactive assignment");
        inactiveAssignment = new PermanentAssignment(user1, role2, meta3);
        ((PermanentAssignment) inactiveAssignment).revoke();
    }

    @Test
    @DisplayName("byUser() should match assignments for specific user")
    void testByUser() {
        AssignmentFilter filter = AssignmentFilters.byUser(user1);
        assertTrue(filter.test(permanentAssignment));
        assertFalse(filter.test(temporaryAssignment));
        assertTrue(filter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("byUsername() should match assignments by username")
    void testByUsername() {
        AssignmentFilter filter = AssignmentFilters.byUsername("john");
        assertTrue(filter.test(permanentAssignment));
        assertFalse(filter.test(temporaryAssignment));
        assertTrue(filter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("byRole() should match assignments for specific role")
    void testByRole() {
        AssignmentFilter filter = AssignmentFilters.byRole(role1);
        assertTrue(filter.test(permanentAssignment));
        assertFalse(filter.test(temporaryAssignment));
        assertFalse(filter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("byRoleName() should match assignments by role name")
    void testByRoleName() {
        AssignmentFilter filter = AssignmentFilters.byRoleName("Admin");
        assertTrue(filter.test(permanentAssignment));
        assertFalse(filter.test(temporaryAssignment));
        assertFalse(filter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("activeOnly() should match only active assignments")
    void testActiveOnly() {
        AssignmentFilter filter = AssignmentFilters.activeOnly();
        assertTrue(filter.test(permanentAssignment));
        assertTrue(filter.test(temporaryAssignment));
        assertFalse(filter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("inactiveOnly() should match only inactive assignments")
    void testInactiveOnly() {
        AssignmentFilter filter = AssignmentFilters.inactiveOnly();
        assertFalse(filter.test(permanentAssignment));
        assertFalse(filter.test(temporaryAssignment));
        assertTrue(filter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("byType() should match assignments by type")
    void testByType() {
        AssignmentFilter permanentFilter = AssignmentFilters.byType("PERMANENT");
        AssignmentFilter temporaryFilter = AssignmentFilters.byType("TEMPORARY");

        assertTrue(permanentFilter.test(permanentAssignment));
        assertFalse(permanentFilter.test(temporaryAssignment));
        assertTrue(permanentFilter.test(inactiveAssignment));

        assertFalse(temporaryFilter.test(permanentAssignment));
        assertTrue(temporaryFilter.test(temporaryAssignment));
        assertFalse(temporaryFilter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("assignedBy() should match assignments by assigner username")
    void testAssignedBy() {
        AssignmentFilter filter = AssignmentFilters.assignedBy("admin");
        assertTrue(filter.test(permanentAssignment));
        assertTrue(filter.test(temporaryAssignment));
        assertTrue(filter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("assignedAfter() should match assignments after specific date")
    void testAssignedAfter() {
        String yesterday = LocalDateTime.now().minusDays(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        AssignmentFilter filter = AssignmentFilters.assignedAfter(yesterday);
        assertTrue(filter.test(permanentAssignment));
        assertTrue(filter.test(temporaryAssignment));
        assertTrue(filter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("expiringBefore() should match temporary assignments expiring before date")
    void testExpiringBefore() {
        String futureDate = LocalDateTime.now().plusDays(31)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

        AssignmentFilter filter = AssignmentFilters.expiringBefore(futureDate);
        assertFalse(filter.test(permanentAssignment));
        assertTrue(filter.test(temporaryAssignment));
        assertFalse(filter.test(inactiveAssignment));
    }

    @Test
    @DisplayName("Filter should work with stream operations")
    void testFilterWithStream() {
        List<RoleAssignment> assignments = Arrays.asList(
                permanentAssignment,
                temporaryAssignment,
                inactiveAssignment
        );

        List<RoleAssignment> active = assignments.stream()
                .filter(AssignmentFilters.activeOnly()::test)
                .toList();

        assertEquals(2, active.size());
        assertTrue(active.contains(permanentAssignment));
        assertTrue(active.contains(temporaryAssignment));
        assertFalse(active.contains(inactiveAssignment));
    }
}
