import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("RoleFilter Tests")
class RoleFilterTest {
    private Permission readPerm;
    private Permission writePerm;
    private Permission deletePerm;
    private Role adminRole;
    private Role editorRole;
    private Role viewerRole;

    @BeforeEach
    void setUp() {
        readPerm = new Permission("READ", "reports", "View reports");
        writePerm = new Permission("WRITE", "reports", "Edit reports");
        deletePerm = new Permission("DELETE", "reports", "Delete reports");

        adminRole = new Role("Admin", "Full access");
        adminRole.addPermission(readPerm);
        adminRole.addPermission(writePerm);
        adminRole.addPermission(deletePerm);

        editorRole = new Role("Editor", "Edit content");
        editorRole.addPermission(readPerm);
        editorRole.addPermission(writePerm);

        viewerRole = new Role("Viewer", "View only");
        viewerRole.addPermission(readPerm);
    }

    @Test
    @DisplayName("byName() should match exact role name")
    void testByNameExactMatch() {
        RoleFilter filter = RoleFilters.byName("Admin");
        assertTrue(filter.test(adminRole));
        assertFalse(filter.test(editorRole));
        assertFalse(filter.test(viewerRole));
    }

    @Test
    @DisplayName("byNameContains() should match substring case-insensitively")
    void testByNameContains() {
        RoleFilter filter = RoleFilters.byNameContains("ed");
        assertFalse(filter.test(adminRole));
        assertTrue(filter.test(editorRole));
        assertFalse(filter.test(viewerRole));
        assertFalse(filter.test(viewerRole));
    }

    @Test
    @DisplayName("hasPermission(Permission) should match role with specific permission object")
    void testHasPermissionWithObject() {
        RoleFilter filter = RoleFilters.hasPermission(readPerm);
        assertTrue(filter.test(adminRole));
        assertTrue(filter.test(editorRole));
        assertTrue(filter.test(viewerRole));
    }

    @Test
    @DisplayName("hasPermission(String, String) should match role with specific permission name and resource")
    void testHasPermissionWithNameAndResource() {
        RoleFilter filter = RoleFilters.hasPermission("WRITE", "reports");
        assertTrue(filter.test(adminRole));
        assertTrue(filter.test(editorRole));
        assertFalse(filter.test(viewerRole));
    }

    @Test
    @DisplayName("hasAtLeastNPermissions() should match roles with minimum permission count")
    void testHasAtLeastNPermissions() {
        RoleFilter filter2 = RoleFilters.hasAtLeastNPermissions(2);
        RoleFilter filter3 = RoleFilters.hasAtLeastNPermissions(3);
        RoleFilter filter4 = RoleFilters.hasAtLeastNPermissions(4);

        assertTrue(filter2.test(adminRole));
        assertTrue(filter2.test(editorRole));
        assertFalse(filter2.test(viewerRole));

        assertTrue(filter3.test(adminRole));
        assertFalse(filter3.test(editorRole));
        assertFalse(filter3.test(viewerRole));

        assertFalse(filter4.test(adminRole));
    }

    @Test
    @DisplayName("and() should combine role filters with logical AND")
    void testAndCombination() {
        RoleFilter hasRead = RoleFilters.hasPermission("READ", "reports");
        RoleFilter hasWrite = RoleFilters.hasPermission("WRITE", "reports");

        RoleFilter combined = hasRead.and(hasWrite);

        assertTrue(combined.test(adminRole));
        assertTrue(combined.test(editorRole));
        assertFalse(combined.test(viewerRole));
    }

    @Test
    @DisplayName("Filter should work with stream operations")
    void testFilterWithStream() {
        List<Role> roles = Arrays.asList(adminRole, editorRole, viewerRole);

        List<Role> filtered = roles.stream()
                .filter(RoleFilters.hasPermission("WRITE", "reports")::test)
                .toList();

        assertEquals(2, filtered.size());
        assertTrue(filtered.contains(adminRole));
        assertTrue(filtered.contains(editorRole));
        assertFalse(filtered.contains(viewerRole));
    }
}
