import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("RoleManager Tests")
class RoleManagerTest {
    private RoleManager roleManager;
    private Role adminRole;
    private Role editorRole;
    private Permission readPerm;
    private Permission writePerm;

    @BeforeEach
    void setUp() {
        roleManager = new RoleManager();

        readPerm = new Permission("READ", "reports", "View reports");
        writePerm = new Permission("WRITE", "reports", "Edit reports");

        adminRole = new Role("Admin", "Full access");
        adminRole.addPermission(readPerm);
        adminRole.addPermission(writePerm);

        editorRole = new Role("Editor", "Edit content");
        editorRole.addPermission(writePerm);
    }

    @Test
    @DisplayName("add() should add role successfully")
    void testAddRole() {
        roleManager.add(adminRole);

        assertTrue(roleManager.exists("Admin"));
        assertEquals(1, roleManager.count());
    }

    @Test
    @DisplayName("add() should throw exception for duplicate role name")
    void testAddDuplicateRole() {
        roleManager.add(adminRole);

        Role duplicate = new Role("Admin", "Different description");

        assertThrows(IllegalArgumentException.class, () -> {
            roleManager.add(duplicate);
        });

        assertEquals(1, roleManager.count());
    }

    @Test
    @DisplayName("remove() should remove role successfully")
    void testRemoveRole() {
        roleManager.add(adminRole);
        roleManager.add(editorRole);

        boolean removed = roleManager.remove(adminRole);

        assertTrue(removed);
        assertFalse(roleManager.exists("Admin"));
        assertEquals(1, roleManager.count());
    }

    @Test
    @DisplayName("remove() should synchronize both maps")
    void testRemoveSynchronizesMaps() {
        roleManager.add(adminRole);

        boolean removed = roleManager.remove(adminRole);
        assertTrue(removed);

        Optional<Role> byId = roleManager.findById(adminRole.id());
        Optional<Role> byName = roleManager.findByName("Admin");

        assertFalse(byId.isPresent());
        assertFalse(byName.isPresent());
    }

    @Test
    @DisplayName("findByName() should return role when exists")
    void testFindByName() {
        roleManager.add(adminRole);

        Optional<Role> found = roleManager.findByName("Admin");

        assertTrue(found.isPresent());
        assertEquals("Full access", found.get().description());
    }

    @Test
    @DisplayName("findByName() should return empty when role not exists")
    void testFindByNameNotFound() {
        Optional<Role> found = roleManager.findByName("Unknown");

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("addPermissionToRole() should add permission successfully")
    void testAddPermissionToRole() {
        roleManager.add(adminRole);

        Permission deletePerm = new Permission("DELETE", "reports", "Delete reports");
        roleManager.addPermissionToRole("Admin", deletePerm);

        assertTrue(adminRole.hasPermission("DELETE", "reports"));
        assertEquals(3, adminRole.getPermissions().size());
    }

    @Test
    @DisplayName("addPermissionToRole() should throw exception for non-existent role")
    void testAddPermissionToNonExistentRole() {
        Permission deletePerm = new Permission("DELETE", "reports", "Delete reports");

        assertThrows(NoSuchElementException.class, () -> {
            roleManager.addPermissionToRole("Unknown", deletePerm);
        });
    }

    @Test
    @DisplayName("removePermissionFromRole() should remove permission successfully")
    void testRemovePermissionFromRole() {
        roleManager.add(adminRole);

        roleManager.removePermissionFromRole("Admin", readPerm);

        assertFalse(adminRole.hasPermission("READ", "reports"));
        assertEquals(1, adminRole.getPermissions().size());
    }

    @Test
    @DisplayName("findByFilter() should filter roles by permission")
    void testFindByFilter() {
        roleManager.add(adminRole);
        roleManager.add(editorRole);

        List<Role> filtered = roleManager.findByFilterParallel(
                role -> role.hasPermission("READ", "reports"),
                null
        );

        assertEquals(1, filtered.size());
        assertEquals("Admin", filtered.get(0).name());
    }

    @Test
    @DisplayName("findAll() with filter and sorter should work correctly")
    void testFindAllWithFilterAndSorter() {
        Role zzzRole = new Role("ZZZ", "Last role");
        zzzRole.addPermission(new Permission("READ", "test", "Test permission"));
        roleManager.add(zzzRole);

        roleManager.add(adminRole);
        roleManager.add(editorRole);

        List<Role> result = roleManager.findAll(
                role -> !role.getPermissions().isEmpty(),
                RoleSorters.byName()
        );

        assertEquals(3, result.size());
        assertEquals("Admin", result.get(0).name());
        assertEquals("Editor", result.get(1).name());
        assertEquals("ZZZ", result.get(2).name());
    }

    @Test
    @DisplayName("findRolesWithPermission() should find roles with specific permission")
    void testFindRolesWithPermission() {
        roleManager.add(adminRole);
        roleManager.add(editorRole);

        List<Role> roles = roleManager.findRolesWithPermission("WRITE", "reports");

        assertEquals(2, roles.size());
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("Admin")));
        assertTrue(roles.stream().anyMatch(r -> r.name().equals("Editor")));
    }

    @Test
    @DisplayName("clear() should remove all roles")
    void testClear() {
        roleManager.add(adminRole);
        roleManager.add(editorRole);

        assertEquals(2, roleManager.count());

        roleManager.clear();

        assertEquals(0, roleManager.count());
        assertTrue(roleManager.findAll(null, null).isEmpty());
    }

    @Test
    @DisplayName("Repository methods should be implemented correctly")
    void testRepositoryMethods() {
        roleManager.add(adminRole);
        assertEquals(1, roleManager.count());

        Optional<Role> foundById = roleManager.findById(adminRole.id());
        assertTrue(foundById.isPresent());

        Optional<Role> foundByName = roleManager.findByName("Admin");
        assertTrue(foundByName.isPresent());

        List<Role> all = roleManager.findAll(null, null);
        assertEquals(1, all.size());

        boolean removed = roleManager.remove(adminRole);
        assertTrue(removed);
        assertEquals(0, roleManager.count());

        roleManager.add(adminRole);
        roleManager.add(editorRole);
        roleManager.clear();
        assertEquals(0, roleManager.count());
    }
}