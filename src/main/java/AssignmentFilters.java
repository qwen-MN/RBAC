import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AssignmentFilters {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static AssignmentFilter byUser(User user) {
        return assignment -> assignment.user().equals(user);
    }

    public static AssignmentFilter byUsername(String username) {
        return assignment -> assignment.user().username().equals(username);
    }

    public static AssignmentFilter byRole(Role role) {
        return assignment -> assignment.role().equals(role);
    }

    public static AssignmentFilter byRoleName(String roleName) {
        return assignment -> assignment.role().name().equals(roleName);
    }

    public static AssignmentFilter activeOnly() {
        return RoleAssignment::isActive;
    }

    public static AssignmentFilter inactiveOnly() {
        return assignment -> !assignment.isActive();
    }

    public static AssignmentFilter byType(String type) {
        return assignment -> assignment.assignmentType().equals(type);
    }

    public static AssignmentFilter assignedBy(String username) {
        return assignment -> assignment.metadata().assignedBy().equals(username);
    }

    public static AssignmentFilter assignedAfter(String date) {
        return assignment -> {
            LocalDateTime assignedAt = LocalDateTime.parse(
                    assignment.metadata().assignedAt(), FORMATTER);
            LocalDateTime threshold = LocalDateTime.parse(date, FORMATTER);
            return assignedAt.isAfter(threshold);
        };
    }

    public static AssignmentFilter expiringBefore(String date) {
        return assignment -> {
            if (!assignment.assignmentType().equals("TEMPORARY")) {
                return false;
            }
            TemporaryAssignment temp = (TemporaryAssignment) assignment;
            LocalDateTime expiry = LocalDateTime.parse(temp.getExpiresAt(), FORMATTER);
            LocalDateTime threshold = LocalDateTime.parse(date, FORMATTER);
            return expiry.isBefore(threshold);
        };
    }
}