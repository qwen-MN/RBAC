import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.function.Predicate;

public class AssignmentManager {
    private final List<RoleAssignment> assignments = new CopyOnWriteArrayList<>();
    private final Map<String, List<RoleAssignment>> assignmentsByUser = new ConcurrentHashMap<>();
    private final Map<String, List<RoleAssignment>> assignmentsByRole = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final UserManager userManager;
    private final RoleManager roleManager;

    public AssignmentManager(UserManager userManager, RoleManager roleManager) {
        this.userManager = userManager;
        this.roleManager = roleManager;
    }

    public void add(RoleAssignment assignment) {
        lock.writeLock().lock();
        try {
            if (!userManager.exists(assignment.user().username())) {
                throw new IllegalArgumentException("User does not exist: " + assignment.user().username());
            }

            if (!roleManager.exists(assignment.role().name())) {
                throw new IllegalArgumentException("Role does not exist: " + assignment.role().name());
            }

            if (userHasRole(assignment.user(), assignment.role())) {
                throw new IllegalArgumentException("User already has active assignment for this role");
            }

            assignments.add(assignment);

            assignmentsByUser.computeIfAbsent(
                    assignment.user().username(),
                    k -> new CopyOnWriteArrayList<>()
            ).add(assignment);

            assignmentsByRole.computeIfAbsent(
                    assignment.role().id(),
                    k -> new CopyOnWriteArrayList<>()
            ).add(assignment);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean remove(RoleAssignment assignment) {
        lock.writeLock().lock();
        try {
            boolean wasRemoved = assignments.remove(assignment);

            if (wasRemoved) {
                assignmentsByUser.getOrDefault(assignment.user().username(), List.of())
                        .remove(assignment);
                assignmentsByRole.getOrDefault(assignment.role().id(), List.of())
                        .remove(assignment);
            }

            return wasRemoved;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findByUser(User user) {
        return assignmentsByUser.getOrDefault(user.username(), List.of());
    }

    public List<RoleAssignment> findByRole(Role role) {
        return assignmentsByRole.getOrDefault(role.id(), List.of());
    }

    public Optional<RoleAssignment> findById(String assignmentId) {
        lock.readLock().lock();
        try {
            return assignments.stream()
                    .filter(assignment -> assignment.assignmentId().equals(assignmentId))
                    .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean userHasRole(User user, Role role) {
        return findByUser(user).stream()
                .anyMatch(a -> a.role().id().equals(role.id()) && a.isActive());
    }

    public Set<Permission> getUserPermissions(User user) {
        return findByUser(user).stream()
                .filter(RoleAssignment::isActive)
                .flatMap(a -> a.role().getPermissions().stream())
                .collect(Collectors.toSet());
    }

    public boolean userHasPermission(User user, String permissionName, String resource) {
        lock.readLock().lock();
        try {
            Set<Permission> userPermissions = getUserPermissions(user);
            return userPermissions.stream()
                    .anyMatch(p ->
                            p.name().equals(permissionName) &&
                                    p.resource().equals(resource)
                    );
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getActiveAssignments() {
        lock.readLock().lock();
        try {
            return assignments.stream()
                    .filter(RoleAssignment::isActive)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> getExpiredAssignments() {
        lock.readLock().lock();
        try {
            return assignments.stream()
                    .filter(assignment -> !assignment.isActive())
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void extendTemporaryAssignment(String assignmentId, String newExpiry) {
        lock.writeLock().lock();
        try {
            Optional<RoleAssignment> assignmentOpt = findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                throw new IllegalArgumentException("Assignment not found: " + assignmentId);
            }

            RoleAssignment assignment = assignmentOpt.get();
            if (!(assignment instanceof TemporaryAssignment)) {
                throw new IllegalArgumentException("Assignment is not temporary: " + assignmentId);
            }

            TemporaryAssignment tempAssignment = (TemporaryAssignment) assignment;

            if (DateUtils.isBefore(newExpiry, tempAssignment.expiresAt())) {
                throw new IllegalArgumentException("New expiry date must be after current expiry date");
            }

            tempAssignment.setExpiresAt(newExpiry);

        } finally {
            lock.writeLock().unlock();
        }
    }

    public void revokeAssignment(String assignmentId) {
        lock.writeLock().lock();
        try {
            Optional<RoleAssignment> assignmentOpt = findById(assignmentId);
            if (assignmentOpt.isEmpty()) {
                throw new IllegalArgumentException("Assignment not found: " + assignmentId);
            }

            RoleAssignment assignment = assignmentOpt.get();
            if (assignment instanceof PermanentAssignment permanent) {
                permanent.revoke(); // Делает назначение неактивным, а не удаляет
            } else {
                throw new IllegalArgumentException("Cannot revoke temporary assignment directly");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<RoleAssignment> findAll(Predicate<RoleAssignment> filter, Comparator<RoleAssignment> sorter) {
        lock.readLock().lock();
        try {
            return assignments.stream()
                    .filter(filter != null ? filter : a -> true)
                    .sorted(sorter != null ? sorter : (a, b) -> 0)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<RoleAssignment> findByFilterParallel(Predicate<RoleAssignment> filter, Comparator<RoleAssignment> sorter) {
        lock.readLock().lock();
        try {
            return assignments.stream()
                    .filter(filter != null ? filter : assignment -> true)
                    .sorted(sorter != null ? sorter : (a, b) -> 0)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public int count() {
        return assignments.size();
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            assignments.clear();
            assignmentsByUser.clear();
            assignmentsByRole.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}