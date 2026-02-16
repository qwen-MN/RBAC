import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public record AssignmentMetadata(String assignedBy, String assignedAt, String reason) {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public AssignmentMetadata {
        Objects.requireNonNull(assignedBy, "AssignedBy cannot be null");
        Objects.requireNonNull(assignedAt, "AssignedAt cannot be null");
        if (reason == null || reason.isBlank()) {
            reason = "No reason provided";
        } else {
            reason = reason.trim();
        }
    }

    public static AssignmentMetadata now(String assignedBy, String reason) {
        String now = LocalDateTime.now().format(FORMATTER);
        return new AssignmentMetadata(assignedBy, now, reason);
    }

    public String format() {
        return String.format("Assigned by: %s at %s\nReason: %s",  assignedBy(), assignedAt(), reason());
    }
}