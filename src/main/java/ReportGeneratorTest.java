import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReportGenerator Tests")
class ReportGeneratorTest {
    private ReportGenerator reportGenerator;
    private UserManager userManager;
    private RoleManager roleManager;
    private AssignmentManager assignmentManager;

    @BeforeEach
    void setUp() {
        reportGenerator = new ReportGenerator();

        userManager = new UserManager();
        roleManager = new RoleManager();
        assignmentManager = new AssignmentManager(userManager, roleManager);

        User testUser = User.create("john", "John Doe", "john@example.com");
        userManager.add(testUser);

        Role adminRole = new Role("Admin", "Administrator role");
        adminRole.addPermission(new Permission("READ", "users", "Read users"));
        roleManager.add(adminRole);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "Test assignment");
        RoleAssignment assignment = new PermanentAssignment(testUser, adminRole, meta);
        assignmentManager.add(assignment);
    }

    @Test
    @DisplayName("generateUserReport() should return formatted string")
    void testGenerateUserReport() {
        String report = reportGenerator.generateUserReport(userManager, assignmentManager);

        assertTrue(report.contains("User Report"), "Should contain report title");
        assertTrue(report.contains("Generated:"), "Should contain timestamp");
        assertTrue(report.contains("john"), "Should contain user data");
    }

    @Test
    @DisplayName("generateRoleReport() should return formatted string")
    void testGenerateRoleReport() {
        String report = reportGenerator.generateRoleReport(roleManager, assignmentManager);

        assertTrue(report.contains("Role Report"), "Should contain report title");
        assertTrue(report.contains("Generated:"), "Should contain timestamp");
        assertTrue(report.contains("Admin"), "Should contain role data");
    }

    @Test
    @DisplayName("generatePermissionMatrix() should return formatted string")
    void testGeneratePermissionMatrix() {
        String report = reportGenerator.generatePermissionMatrix(userManager, assignmentManager);

        assertTrue(report.contains("Permission Matrix"), "Should contain matrix title");
        assertTrue(report.contains("Generated:"), "Should contain timestamp");
        assertTrue(report.contains("john"), "Should contain user data");
        assertTrue(report.contains("users"), "Should contain resource data");
    }
}