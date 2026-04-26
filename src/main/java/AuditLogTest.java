import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AuditLog Tests")
class AuditLogTest {
    private AuditLog auditLog;

    @BeforeEach
    void setUp() {
        auditLog = new AuditLog();
    }

    @Test
    @DisplayName("log() should add entry to log")
    void testLogEntryAdded() throws InterruptedException {
        auditLog.log("CREATE_USER", "admin", "john", "Created user john");

        auditLog.waitForProcessing();

        List<AuditLog.AuditEntry> entries = auditLog.getAll();
        assertEquals(1, entries.size());

        AuditLog.AuditEntry entry = entries.get(0);
        assertEquals("CREATE_USER", entry.action());
        assertEquals("admin", entry.performer());
        assertEquals("john", entry.target());
        assertEquals("Created user john", entry.details());
        assertNotNull(entry.timestamp());
    }

    @Test
    @DisplayName("getByPerformer() should filter by performer")
    void testGetByPerformer() throws InterruptedException {
        auditLog.log("CREATE_USER", "admin", "john", "Created user john");
        auditLog.log("CREATE_USER", "admin", "jane", "Created user jane");
        auditLog.log("CREATE_USER", "system", "test", "System user");

        auditLog.waitForProcessing();

        List<AuditLog.AuditEntry> adminEntries = auditLog.getByPerformer("admin");
        assertEquals(2, adminEntries.size());

        List<AuditLog.AuditEntry> systemEntries = auditLog.getByPerformer("system");
        assertEquals(1, systemEntries.size());
    }

    @Test
    @DisplayName("getByAction() should filter by action")
    void testGetByAction() throws InterruptedException {
        auditLog.log("CREATE_USER", "admin", "john", "Created user john");
        auditLog.log("DELETE_USER", "admin", "john", "Deleted user john");
        auditLog.log("CREATE_USER", "admin", "jane", "Created user jane");

        auditLog.waitForProcessing();

        List<AuditLog.AuditEntry> createEntries = auditLog.getByAction("CREATE_USER");
        assertEquals(2, createEntries.size());

        List<AuditLog.AuditEntry> deleteEntries = auditLog.getByAction("DELETE_USER");
        assertEquals(1, deleteEntries.size());
    }

    @Test
    @DisplayName("getAll() should return all entries")
    void testGetAll() throws InterruptedException {
        auditLog.log("CREATE_USER", "admin", "john", "Created user john");
        auditLog.log("CREATE_ROLE", "admin", "Editor", "Created role Editor");

        auditLog.waitForProcessing();

        List<AuditLog.AuditEntry> entries = auditLog.getAll();
        assertEquals(2, entries.size());
    }
}