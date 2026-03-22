import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("RoleSorters Tests")
class RoleSortersTest {
    private Role role1;
    private Role role2;
    private Role role3;

    @BeforeEach
    void setUp() {
        role1 = new Role("Admin", "Full access");
        role1.addPermission(new Permission("READ", "reports", "View reports"));
        role1.addPermission(new Permission("WRITE", "reports", "Edit reports"));
        role1.addPermission(new Permission("DELETE", "reports", "Delete reports"));

        role2 = new Role("Editor", "Edit content");
        role2.addPermission(new Permission("READ", "reports", "View reports"));
        role2.addPermission(new Permission("WRITE", "reports", "Edit reports"));

        role3 = new Role("Viewer", "View only");
        role3.addPermission(new Permission("READ", "reports", "View reports"));
    }

    @Test
    @DisplayName("byName() should sort roles by name alphabetically")
    void testSortByName() {
        List<Role> roles = Arrays.asList(role1, role2, role3);

        roles.sort(RoleSorters.byName());

        assertEquals("Admin", roles.get(0).name());
        assertEquals("Editor", roles.get(1).name());
        assertEquals("Viewer", roles.get(2).name());
    }

    @Test
    @DisplayName("byPermissionCount() should sort roles by permission count descending")
    void testSortByPermissionCount() {
        List<Role> roles = Arrays.asList(role3, role1, role2);

        roles.sort(RoleSorters.byPermissionCount());

        assertEquals("Admin", roles.get(0).name());
        assertEquals("Editor", roles.get(1).name());
        assertEquals("Viewer", roles.get(2).name());
    }

    @Test
    @DisplayName("byPermissionCount() should handle roles with equal permission counts")
    void testSortByPermissionCountEqual() {
        Role role4 = new Role("Reader", "Read only");
        role4.addPermission(new Permission("READ", "dashboards", "View dashboards"));

        List<Role> roles = Arrays.asList(role3, role4);

        roles.sort(RoleSorters.byPermissionCount());

        assertEquals(2, roles.size());
        assertTrue(roles.get(0).name().equals("Viewer") || roles.get(0).name().equals("Reader"));
    }
}