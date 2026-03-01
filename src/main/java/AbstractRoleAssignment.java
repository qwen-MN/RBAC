import java.util.Objects;
import java.util.UUID;

public abstract class AbstractRoleAssignment implements RoleAssignment {
    private final String assignmentId;
    private final User user;
    private final Role role;
    private final AssignmentMetadata metadata;

    protected AbstractRoleAssignment(User user, Role role, AssignmentMetadata metadata) {
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(role, "Role cannot be null");
        Objects.requireNonNull(metadata, "Metadata cannot be null");

        this.assignmentId = "assign_" + UUID.randomUUID().toString().substring(0, 8);
        this.user = user;
        this.role = role;
        this.metadata = metadata;
    }

    @Override
    public String assignmentId() {
        return assignmentId;
    }

    @Override
    public User user() {
        return user;
    }

    @Override
    public Role role() {
        return role;
    }

    @Override
    public AssignmentMetadata metadata() {
        return metadata;
    }

    public String summary() {
        String status = isActive() ? "ACTIVE" : "INACTIVE";
        return String.format("[%s] %s assigned to %s by %s at %s\nReason: %s\nStatus: %s",
                assignmentType(),
                role().name(),
                user().username(),
                metadata().assignedBy(),
                metadata().assignedAt(),
                metadata().reason(),
                status
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractRoleAssignment that)) {
            return false;
        }
        return Objects.equals(assignmentId, that.assignmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignmentId);
    }
}