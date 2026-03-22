import java.util.List;
import java.util.stream.Collectors;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
        this.currentUser = null;
    }

    public void initialize() {
        Permission readReports = new Permission("READ", "reports", "View reports");
        Permission writeReports = new Permission("WRITE", "reports", "Edit reports");
        Permission deleteReports = new Permission("DELETE", "reports", "Delete reports");
        Permission readDashboards = new Permission("READ", "dashboards", "View dashboards");

        Role adminRole = new Role("Admin", "Full access");
        adminRole.addPermission(readReports);
        adminRole.addPermission(writeReports);
        adminRole.addPermission(deleteReports);
        adminRole.addPermission(readDashboards);

        Role managerRole = new Role("Manager", "Manage reports");
        managerRole.addPermission(readReports);
        managerRole.addPermission(writeReports);

        Role viewerRole = new Role("Viewer", "View-only access");
        viewerRole.addPermission(readReports);
        viewerRole.addPermission(readDashboards);

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        User admin = User.create("admin", "System Administrator", "admin@rbac.system");
        userManager.add(admin);

        AssignmentMetadata meta = AssignmentMetadata.now("system", "Initial setup");
        PermanentAssignment assignment = new PermanentAssignment(admin, adminRole, meta);
        assignmentManager.add(assignment);

        setCurrentUser("admin");
    }

    public String generateStatistics() {
        int usersCount = userManager.count();
        int rolesCount = roleManager.count();
        int assignmentsCount = assignmentManager.count();
        int activeAssignments = assignmentManager.getActiveAssignments().size();
        int expiredAssignments = assignmentManager.getExpiredAssignments().size();

        double avgRolesPerUser = usersCount > 0 ?
                (double) assignmentsCount / usersCount : 0;

        List<String> topRoles = roleManager.findAll(null, null)
                .stream()
                .sorted((r1, r2) ->
                        Integer.compare(
                                assignmentManager.findByRole(r2).size(),
                                assignmentManager.findByRole(r1).size()
                        ))
                .limit(3)
                .map(Role::name)
                .collect(Collectors.toList());

        return String.format(
                "=== Статистика системы ===%n" +
                        "Пользователей: %d%n" +
                        "Ролей: %d%n" +
                        "Назначений всего: %d (активных: %d, истёкших: %d)%n" +
                        "Среднее количество ролей на пользователя: %.2f%n" +
                        "Топ-3 ролей: %s",
                usersCount, rolesCount, assignmentsCount, activeAssignments,
                expiredAssignments, avgRolesPerUser,
                topRoles.isEmpty() ? "Нет" : String.join(", ", topRoles)
        );
    }

    public UserManager getUserManager() {
        return userManager;
    }
    public RoleManager getRoleManager() {
        return roleManager;
    }
    public AssignmentManager getAssignmentManager() {
        return assignmentManager;
    }
    public String getCurrentUser() {
        return currentUser;
    }
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }
}