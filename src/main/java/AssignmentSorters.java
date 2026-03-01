import java.util.Comparator;

public class AssignmentSorters {

    public static Comparator<RoleAssignment> byUsername() {
        return Comparator.comparing(assignment -> assignment.user().username());
    }

    public static Comparator<RoleAssignment> byRoleName() {
        return Comparator.comparing(assignment -> assignment.role().name());
    }

    public static Comparator<RoleAssignment> byAssignmentDate() {
        return Comparator.comparing(assignment -> assignment.metadata().assignedAt());
    }
}
