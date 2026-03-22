import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("AssignmentSorters Tests")
class AssignmentSortersTest {
    private User user1;
    private User user2;
    private Role role1;
    private Role role2;
    private RoleAssignment assignment1;
    private RoleAssignment assignment2;

    @BeforeEach
    void setUp() {
        user1 = User.create("alice", "Alice Johnson", "alice@example.com");
        user2 = User.create("bob", "Bob Smith", "bob@example.com");

        role1 = new Role("Admin", "Full access");
        role2 = new Role("Editor", "Edit content");

        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "First assignment");
        assignment1 = new PermanentAssignment(user1, role1, meta1);

        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Second assignment");
        assignment2 = new PermanentAssignment(user2, role2, meta2);
    }

    @Test
    @DisplayName("byUsername() should sort assignments by username alphabetically")
    void testSortByUsername() {
        List<RoleAssignment> assignments = Arrays.asList(assignment2, assignment1);

        assignments.sort(AssignmentSorters.byUsername());

        assertEquals("alice", assignments.get(0).user().username());
        assertEquals("bob", assignments.get(1).user().username());
    }

    @Test
    @DisplayName("byRoleName() should sort assignments by role name alphabetically")
    void testSortByRoleName() {
        List<RoleAssignment> assignments = Arrays.asList(assignment2, assignment1);

        assignments.sort(AssignmentSorters.byRoleName());

        assertEquals("Admin", assignments.get(0).role().name());
        assertEquals("Editor", assignments.get(1).role().name());
    }

    @Test
    @DisplayName("byAssignmentDate() should sort assignments by assignment date")
    void testSortByAssignmentDate() {
        AssignmentMetadata metaEarly = new AssignmentMetadata(
                "admin", "2026-01-01 10:00", "Early assignment");
        AssignmentMetadata metaLate = new AssignmentMetadata(
                "admin", "2026-01-02 10:00", "Late assignment");

        RoleAssignment early = new PermanentAssignment(user1, role1, metaEarly);
        RoleAssignment late = new PermanentAssignment(user2, role2, metaLate);

        List<RoleAssignment> assignments = Arrays.asList(late, early);

        assignments.sort(AssignmentSorters.byAssignmentDate());

        assertEquals("2026-01-01", assignments.get(0).metadata().assignedAt().substring(0, 10));
        assertEquals("2026-01-02", assignments.get(1).metadata().assignedAt().substring(0, 10));
    }
}
