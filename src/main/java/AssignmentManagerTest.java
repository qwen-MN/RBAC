import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("AssignmentManager Tests")
class AssignmentManagerTest {
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;
    private User testUser;
    private Role adminRole;
    private Role editorRole;

    @BeforeEach
    void setUp() {
        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);

        testUser = User.create("john", "John Doe", "john@example.com");
        userManager.add(testUser);

        adminRole = new Role("Admin", "Full access");
        adminRole.addPermission(new Permission("READ", "reports", "View reports"));
        roleManager.add(adminRole);

        editorRole = new Role("Editor", "Edit content");
        editorRole.addPermission(new Permission("WRITE", "reports", "Edit reports"));
        roleManager.add(editorRole);
    }

    @Test
    @DisplayName("add() should add assignment successfully")
    void testAddAssignment() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test assignment");
        PermanentAssignment assignment = new PermanentAssignment(testUser, adminRole, meta);

        assignmentManager.add(assignment);

        assertEquals(1, assignmentManager.count());
        assertTrue(assignmentManager.findByUser(testUser).contains(assignment));
    }

    @Test
    @DisplayName("add() should throw exception for non-existent user")
    void testAddAssignmentWithNonExistentUser() {
        User unknownUser = User.create("unknown", "Unknown", "unknown@example.com");

        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(unknownUser, adminRole, meta);

        assertThrows(IllegalArgumentException.class, () -> {
            assignmentManager.add(assignment);
        });
    }

    @Test
    @DisplayName("add() should throw exception for non-existent role")
    void testAddAssignmentWithNonExistentRole() {
        Role unknownRole = new Role("Unknown", "Unknown role");

        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(testUser, unknownRole, meta);

        assertThrows(IllegalArgumentException.class, () -> {
            assignmentManager.add(assignment);
        });
    }

    @Test
    @DisplayName("add() should throw exception for duplicate active assignment")
    void testAddDuplicateActiveAssignment() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "First");
        PermanentAssignment assignment1 = new PermanentAssignment(testUser, adminRole, meta1);
        assignmentManager.add(assignment1);

        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Second");
        PermanentAssignment assignment2 = new PermanentAssignment(testUser, adminRole, meta2);

        assertThrows(IllegalArgumentException.class, () -> {
            assignmentManager.add(assignment2);
        });

        assertEquals(1, assignmentManager.count());
    }

    @Test
    @DisplayName("remove() should remove assignment successfully")
    void testRemoveAssignment() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(testUser, adminRole, meta);
        assignmentManager.add(assignment);

        boolean removed = assignmentManager.remove(assignment);

        assertTrue(removed);
        assertEquals(0, assignmentManager.count());
    }

    @Test
    @DisplayName("findByUser() should return all assignments for user")
    void testFindByUser() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "First");
        PermanentAssignment assignment1 = new PermanentAssignment(testUser, adminRole, meta1);
        assignmentManager.add(assignment1);

        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Second");
        PermanentAssignment assignment2 = new PermanentAssignment(testUser, editorRole, meta2);
        assignmentManager.add(assignment2);

        List<RoleAssignment> assignments = assignmentManager.findByUser(testUser);

        assertEquals(2, assignments.size());
        assertTrue(assignments.contains(assignment1));
        assertTrue(assignments.contains(assignment2));
    }

    @Test
    @DisplayName("findByRole() should return all assignments for role")
    void testFindByRole() {
        User user2 = User.create("jane", "Jane Smith", "jane@example.com");
        userManager.add(user2);

        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "First");
        PermanentAssignment assignment1 = new PermanentAssignment(testUser, adminRole, meta1);
        assignmentManager.add(assignment1);

        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Second");
        PermanentAssignment assignment2 = new PermanentAssignment(user2, adminRole, meta2);
        assignmentManager.add(assignment2);

        List<RoleAssignment> assignments = assignmentManager.findByRole(adminRole);

        assertEquals(2, assignments.size());
        assertTrue(assignments.contains(assignment1));
        assertTrue(assignments.contains(assignment2));
    }

    @Test
    @DisplayName("getActiveAssignments() should return only active assignments")
    void testGetActiveAssignments() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Active");
        PermanentAssignment active = new PermanentAssignment(testUser, adminRole, meta1);
        assignmentManager.add(active);

        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Revoked");
        PermanentAssignment revoked = new PermanentAssignment(testUser, editorRole, meta2);
        assignmentManager.add(revoked);
        revoked.revoke();

        List<RoleAssignment> activeAssignments = assignmentManager.getActiveAssignments();

        assertEquals(1, activeAssignments.size());
        assertTrue(activeAssignments.contains(active));
        assertFalse(activeAssignments.contains(revoked));
    }

    @Test
    @DisplayName("userHasRole() should return true when user has role")
    void testUserHasRole() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(testUser, adminRole, meta);
        assignmentManager.add(assignment);

        assertTrue(assignmentManager.userHasRole(testUser, adminRole));
        assertFalse(assignmentManager.userHasRole(testUser, editorRole));
    }

    @Test
    @DisplayName("userHasPermission() should return true when user has permission")
    void testUserHasPermission() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(testUser, adminRole, meta);
        assignmentManager.add(assignment);

        assertTrue(assignmentManager.userHasPermission(testUser, "READ", "reports"));
        assertFalse(assignmentManager.userHasPermission(testUser, "WRITE", "reports"));
    }

    @Test
    @DisplayName("getUserPermissions() should return all permissions from all roles")
    void testGetUserPermissions() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "Admin role");
        PermanentAssignment adminAssignment = new PermanentAssignment(testUser, adminRole, meta1);
        assignmentManager.add(adminAssignment);

        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Editor role");
        PermanentAssignment editorAssignment = new PermanentAssignment(testUser, editorRole, meta2);
        assignmentManager.add(editorAssignment);

        Set<Permission> permissions = assignmentManager.getUserPermissions(testUser);

        assertEquals(2, permissions.size());
        assertTrue(permissions.stream().anyMatch(p -> p.name().equals("READ")));
        assertTrue(permissions.stream().anyMatch(p -> p.name().equals("WRITE")));
    }

    @Test
    @DisplayName("findByFilter() should filter assignments by user")
    void testFindByFilter() {
        AssignmentMetadata meta1 = AssignmentMetadata.now("admin", "First");
        PermanentAssignment assignment1 = new PermanentAssignment(testUser, adminRole, meta1);
        assignmentManager.add(assignment1);

        User user2 = User.create("jane", "Jane Smith", "jane@example.com");
        userManager.add(user2);

        AssignmentMetadata meta2 = AssignmentMetadata.now("admin", "Second");
        PermanentAssignment assignment2 = new PermanentAssignment(user2, editorRole, meta2);
        assignmentManager.add(assignment2);

        List<RoleAssignment> filtered = assignmentManager.findByFilterParallel(
                assignment -> assignment.user().username().equalsIgnoreCase("john"),
                null
        );

        assertEquals(1, filtered.size());
        assertEquals(assignment1, filtered.getFirst());
    }

    @Test
    @DisplayName("revokeAssignment() should revoke permanent assignment")
    void testRevokeAssignment() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(testUser, adminRole, meta);
        assignmentManager.add(assignment);

        assignmentManager.revokeAssignment(assignment.assignmentId());

        var revokedAssignment = assignmentManager.findById(assignment.assignmentId()).get();
        assertFalse(revokedAssignment.isActive());

        assertTrue(assignmentManager.getExpiredAssignments().contains(revokedAssignment));
    }

    @Test
    @DisplayName("extendTemporaryAssignment() should extend temporary assignment")
    void testExtendTemporaryAssignment() {
        String expiresAt = "2026-12-31 23:59";
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        TemporaryAssignment assignment = new TemporaryAssignment(
                testUser, adminRole, meta, expiresAt, false
        );
        assignmentManager.add(assignment);

        String newExpiry = "2027-12-31 23:59";
        assignmentManager.extendTemporaryAssignment(assignment.assignmentId(), newExpiry);

        var updatedAssignment = assignmentManager.findById(assignment.assignmentId()).get();
        assertEquals(newExpiry, ((TemporaryAssignment) updatedAssignment).expiresAt());
    }

    @Test
    @DisplayName("Repository methods should be implemented correctly")
    void testRepositoryMethods() {
        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Test");
        PermanentAssignment assignment = new PermanentAssignment(testUser, adminRole, meta);

        assignmentManager.add(assignment);
        assertEquals(1, assignmentManager.count());

        var found = assignmentManager.findById(assignment.assignmentId());
        assertTrue(found.isPresent());

        List<RoleAssignment> all = assignmentManager.findAll(null, null);
        assertEquals(1, all.size());

        boolean removed = assignmentManager.remove(assignment);
        assertTrue(removed);
        assertEquals(0, assignmentManager.count());

        assignmentManager.add(assignment);
        assignmentManager.clear();
        assertEquals(0, assignmentManager.count());
    }
}