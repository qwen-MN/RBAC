import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AuditLog {

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {}

    private final List<AuditEntry> entries = new ArrayList<>();
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void log(String action, String performer, String target, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
        entries.add(new AuditEntry(timestamp, action, performer, target, details));
    }

    public List<AuditEntry> getAll() {
        return new ArrayList<>(entries);
    }

    public List<AuditEntry> getByPerformer(String performer) {
        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (entry.performer().equals(performer)) {
                result.add(entry);
            }
        }
        return result;
    }

    public List<AuditEntry> getByAction(String action) {
        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (entry.action().equals(action)) {
                result.add(entry);
            }
        }
        return result;
    }

    public void printLog() {
        System.out.println(FormatUtils.formatHeader("Audit Log"));

        if (entries.isEmpty()) {
            System.out.println("No audit entries found");
            return;
        }

        String[] headers = {"Timestamp", "Action", "Performer", "Target", "Details"};
        List<String[]> rows = new ArrayList<>();

        for (AuditEntry entry : entries) {
            rows.add(new String[]{
                    entry.timestamp(),
                    entry.action(),
                    entry.performer(),
                    entry.target(),
                    entry.details()
            });
        }

        System.out.println(FormatUtils.formatTable(headers, rows));
    }

    public void saveToFile(String filename) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("=== Audit Log ===\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "\n\n");

            for (AuditEntry entry : entries) {
                writer.write(String.format(
                        "[%s] %s | %s | %s | %s%n",
                        entry.timestamp(),
                        entry.action(),
                        entry.performer(),
                        entry.target(),
                        entry.details()
                ));
            }
        }
    }
}