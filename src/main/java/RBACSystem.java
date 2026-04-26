import java.util.List;
import java.util.stream.Collectors;

public class RBACSystem {
    private final UserManager userManager;
    private final RoleManager roleManager;
    private final AssignmentManager assignmentManager;
    private String currentUser;
    private final AuditLog auditLog;
    private final ReportGenerator reportGenerator;
    private final BackgroundExecutor backgroundExecutor;

    public RBACSystem() {
        this.userManager = new UserManager();
        this.roleManager = new RoleManager();
        this.assignmentManager = new AssignmentManager(userManager, roleManager);
        this.currentUser = null;
        this.auditLog = new AuditLog();
        this.reportGenerator = new ReportGenerator();
        this.backgroundExecutor = new BackgroundExecutor();
    }

    public void initialize() {
        Role adminRole = getAdminRole();

        Role managerRole = getManagerRole();

        Role viewerRole = new Role("Viewer", "Только просмотр информации");
        viewerRole.addPermission(new Permission("READ", "users", "Просмотр списка пользователей"));
        viewerRole.addPermission(new Permission("READ", "reports", "Просмотр отчетов"));

        roleManager.add(adminRole);
        roleManager.add(managerRole);
        roleManager.add(viewerRole);

        User adminUser = User.create("admin", "Системный Администратор", "admin@example.com");
        userManager.add(adminUser);

        AssignmentMetadata metadata = AssignmentMetadata.now("system", "Инициализация системы");
        RoleAssignment adminAssignment = new PermanentAssignment(adminUser, adminRole, metadata);
        assignmentManager.add(adminAssignment);
    }

    private static Role getManagerRole() {
        Role managerRole = new Role("Manager", "Управление пользователями и отчетами");
        managerRole.addPermission(new Permission("READ", "users", "Чтение пользователей"));
        managerRole.addPermission(new Permission("WRITE", "users", "Создание/редактирование пользователей"));
        managerRole.addPermission(new Permission("READ", "reports", "Просмотр отчетов"));
        managerRole.addPermission(new Permission("EXPORT", "reports", "Экспорт отчетов"));
        managerRole.addPermission(new Permission("ASSIGN", "roles", "Назначение ролей пользователям"));
        return managerRole;
    }

    private static Role getAdminRole() {
        Role adminRole = new Role("Admin", "Полный доступ ко всем функциям системы");
        adminRole.addPermission(new Permission("READ", "users", "Чтение пользователей"));
        adminRole.addPermission(new Permission("WRITE", "users", "Создание/редактирование пользователей"));
        adminRole.addPermission(new Permission("DELETE", "users", "Удаление пользователей"));
        adminRole.addPermission(new Permission("READ", "roles", "Чтение ролей"));
        adminRole.addPermission(new Permission("WRITE", "roles", "Создание/редактирование ролей"));
        adminRole.addPermission(new Permission("DELETE", "roles", "Удаление ролей"));
        adminRole.addPermission(new Permission("ASSIGN", "roles", "Назначение ролей пользователям"));
        adminRole.addPermission(new Permission("REVOKE", "roles", "Отзыв ролей у пользователей"));
        adminRole.addPermission(new Permission("READ", "reports", "Просмотр всех отчетов"));
        adminRole.addPermission(new Permission("WRITE", "reports", "Редактирование отчетов"));
        adminRole.addPermission(new Permission("DELETE", "reports", "Удаление отчетов"));
        adminRole.addPermission(new Permission("READ", "dashboards", "Просмотр дашбордов"));
        adminRole.addPermission(new Permission("WRITE", "dashboards", "Редактирование дашбордов"));
        adminRole.addPermission(new Permission("DELETE", "dashboards", "Удаление дашбордов"));
        adminRole.addPermission(new Permission("EXPORT", "reports", "Экспорт отчетов"));
        adminRole.addPermission(new Permission("AUDIT", "system", "Просмотр аудит-лога"));
        return adminRole;
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
        return currentUser != null ? currentUser : "system";
    }
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }

    public AuditLog getAuditLog() {
        return auditLog;
    }

    public ReportGenerator getReportGenerator() {
        return reportGenerator;
    }

    public BackgroundExecutor getBackgroundExecutor() {
        return backgroundExecutor;
    }

    public void shutdown() {
        auditLog.shutdown();
        backgroundExecutor.shutdown();
    }
}