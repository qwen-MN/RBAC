import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RBACSystem Tests")
class RBACSystemTest {
    private RBACSystem system;

    @BeforeEach
    void setUp() {
        system = new RBACSystem();
        system.initialize();
    }

    @Test
    @DisplayName("Managers should be initialized")
    void testManagersInitialized() {
        assertNotNull(system.getUserManager(), "UserManager should be initialized");
        assertNotNull(system.getRoleManager(), "RoleManager should be initialized");
        assertNotNull(system.getAssignmentManager(), "AssignmentManager should be initialized");
    }

    @Test
    @DisplayName("Default roles should exist after initialization")
    void testDefaultRolesCreated() {
        assertTrue(system.getRoleManager().exists("Admin"), "Admin role should exist");
        assertTrue(system.getRoleManager().exists("Manager"), "Manager role should exist");
        assertTrue(system.getRoleManager().exists("Viewer"), "Viewer role should exist");
    }

    @Test
    @DisplayName("Admin user should exist after initialization")
    void testAdminUserCreated() {
        assertTrue(system.getUserManager().exists("admin"), "Admin user should exist");
    }

    @Test
    @DisplayName("Admin user should have Admin role")
    void testAdminHasAdminRole() {
        var admin = system.getUserManager().findByUsername("admin").orElseThrow();
        var adminRole = system.getRoleManager().findByName("Admin").orElseThrow();
        assertTrue(system.getAssignmentManager().userHasRole(admin, adminRole),
                "Admin user should have Admin role");
    }

    @Test
    @DisplayName("CurrentUser should be set to admin")
    void testCurrentUserIsAdmin() {
        assertEquals("admin", system.getCurrentUser(), "Current user should be admin");
    }

    @Test
    @DisplayName("Statistics should contain required metrics")
    void testGenerateStatistics() {
        String stats = system.generateStatistics();

        assertTrue(stats.contains("Пользователей: 1"), "Should show user count");
        assertTrue(stats.contains("Ролей: 3"), "Should show role count");
        assertTrue(stats.contains("Назначений всего: 1"), "Should show assignment count");
        assertTrue(stats.contains("активных: 1"), "Should show active assignments");
        assertTrue(stats.contains("Топ-3 ролей:"), "Should show top roles");
    }

    @Test
    @DisplayName("Admin role should have all required permissions")
    void testAdminRolePermissions() {
        var adminRole = system.getRoleManager().findByName("Admin").orElseThrow();

        assertTrue(adminRole.hasPermission("READ", "reports"), "Admin should have READ reports");
        assertTrue(adminRole.hasPermission("WRITE", "reports"), "Admin should have WRITE reports");
        assertTrue(adminRole.hasPermission("DELETE", "reports"), "Admin should have DELETE reports");
        assertTrue(adminRole.hasPermission("READ", "dashboards"), "Admin should have READ dashboards");
    }

    @Test
    @DisplayName("Viewer role should have only read permissions")
    void testViewerRolePermissions() {
        var viewerRole = system.getRoleManager().findByName("Viewer").orElseThrow();

        assertTrue(viewerRole.hasPermission("READ", "reports"), "Viewer should have READ reports");
        assertTrue(viewerRole.hasPermission("READ", "dashboards"), "Viewer should have READ dashboards");
        assertFalse(viewerRole.hasPermission("WRITE", "reports"), "Viewer should not have WRITE reports");
        assertFalse(viewerRole.hasPermission("DELETE", "reports"), "Viewer should not have DELETE reports");
    }

    @Test
    @DisplayName("Manager role should have read/write but not delete")
    void testManagerRolePermissions() {
        var managerRole = system.getRoleManager().findByName("Manager").orElseThrow();

        assertTrue(managerRole.hasPermission("READ", "reports"), "Manager should have READ reports");
        assertTrue(managerRole.hasPermission("WRITE", "reports"), "Manager should have WRITE reports");
        assertFalse(managerRole.hasPermission("DELETE", "reports"), "Manager should not have DELETE reports");
    }

    @Test
    @DisplayName("setCurrentUser should update current user")
    void testSetCurrentUser() {
        system.setCurrentUser("testuser");
        assertEquals("testuser", system.getCurrentUser(), "Current user should be updated");
    }
}