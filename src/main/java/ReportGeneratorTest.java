import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ReportGenerator Tests")
class ReportGeneratorTest {
    private ReportGenerator reportGenerator = new ReportGenerator();

    @Test
    @DisplayName("generateUserReport() should return formatted string")
    void testGenerateUserReport() {
        String report = reportGenerator.generateUserReport(null, null);

        assertTrue(report.contains("User Report"), "Should contain report title");
        assertTrue(report.contains("Generated:"), "Should contain timestamp");
    }

    @Test
    @DisplayName("generateRoleReport() should return formatted string")
    void testGenerateRoleReport() {
        String report = reportGenerator.generateRoleReport(null, null);

        assertTrue(report.contains("Role Report"), "Should contain report title");
        assertTrue(report.contains("Generated:"), "Should contain timestamp");
    }

    @Test
    @DisplayName("generatePermissionMatrix() should return formatted string")
    void testGeneratePermissionMatrix() {
        String report = reportGenerator.generatePermissionMatrix(null, null);

        assertTrue(report.contains("Permission Matrix"), "Should contain matrix title");
        assertTrue(report.contains("Generated:"), "Should contain timestamp");
    }
}