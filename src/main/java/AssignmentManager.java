import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AssignmentManager implements Repository<RoleAssignment> {

    private final Map<String, RoleAssignment> assignments = new HashMap<>();
    private final UserManager userManager;
    private final RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = Objects.requireNonNull(userManager, "UserManager cannot be null");
        this.roleManager = Objects.requireNonNull(roleManager, "RoleManager cannot be null");
    }

    @Override
    public void add(RoleAssignment assignment) {
        Objects.requireNonNull(assignment, "Assignment cannot be null");

        if (!userManager.exists(assignment.user().username())) {
            throw new IllegalArgumentException(
                    "Cannot assign role: role '" + assignment.role().name() + "' does not exist"
            );
        }

        boolean hasActiveAssignment = assignments.values().stream()
                .anyMatch(existing ->
                        existing.isActive() &&
                                existing.user().username().equals(assignment.user().username()) &&
                                existing.role().name().equals(assignment.role().name())
                );

        if (hasActiveAssignment) {
            throw new IllegalArgumentException(
                    "User '" + assignment.user().username() +
                            "' already has an active assignment of role '" +
                            assignment.role().name() + "'"
            );
        }

        assignments.put(assignment.assignmentId(), assignment);
    }

    @Override
    public boolean remove(RoleAssignment assignment) {
        return assignments.remove(assignment.assignmentId()) != null;
    }

    @Override
    public Optional<RoleAssignment> findById(String assignmentId) {
        return Optional.ofNullable(assignments.get(assignmentId));
    }

    @Override
    public List<RoleAssignment> findAll() {
        return new ArrayList<>(assignments.values());
    }

    @Override
    public int count() {
        return assignments.size();
    }

    @Override
    public void clear() {
        assignments.clear();
    }

    public List<RoleAssignment> findByUser(User user) {
        return findByFilter(AssignmentFilters.byUser(user));
    }

    public List<RoleAssignment> findByRole(Role role) {
        return findByFilter(AssignmentFilters.byRole(role));
    }

    public List<RoleAssignment> findByFilter(AssignmentFilter filter) {
        return findAll(filter, null);
    }

    public List<RoleAssignment> findAll(AssignmentFilter filter,
                                        Comparator<RoleAssignment> sorter) {
        Stream<RoleAssignment> stream = assignments.values().stream();

        if (filter != null) {
            stream = stream.filter(filter::test);
        }

        if (sorter != null) {
            stream = stream.sorted(sorter);
        }

        return stream.collect(Collectors.toList());
    }

    public List<RoleAssignment> getActiveAssignments() {
        return findByFilter(AssignmentFilters.activeOnly());
    }

    public List<RoleAssignment> getExpiredAssignments() {
        return findByFilter(AssignmentFilters.inactiveOnly());
    }

    public boolean userHasRole(User user, Role role) {
        return findByUser(user).stream()
                .anyMatch(assignment ->
                        assignment.isActive() &&
                                assignment.role().equals(role)
                );
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        return getUserPermissions(user).stream()
                .anyMatch(permission ->
                        permission.name().equals(permissionName.toUpperCase(Locale.ROOT)) &&
                                permission.resource().equals(resource.toLowerCase(Locale.ROOT))
                );
    }

    public Set<Permission> getUserPermissions(User user) {
        Set<Permission> permissions = new HashSet<>();

        findByUser(user).stream()
                .filter(RoleAssignment::isActive)
                .map(assignment -> assignment.role().getPermissions())
                .forEach(permissions::addAll);

        return permissions;
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpirationDate) {
        RoleAssignment assignment = findById(assignmentId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Assignment with ID '" + assignmentId + "' not found"));

        if (assignment instanceof TemporaryAssignment temp) {
            temp.extend(newExpirationDate);
        } else {
            throw new IllegalArgumentException(
                    "Only temporary assignments can be extended");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssignmentManager that)) return false;
        return Objects.equals(assignments, that.assignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assignments);
    }
}
