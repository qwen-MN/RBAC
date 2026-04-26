import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportGenerator {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public String generateUserReport(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append(FormatUtils.formatHeader("User Report"))
                .append("\nGenerated: ")
                .append(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .append("\n\n");

        List<User> users = userManager.findAll(null, null);

        if (users.isEmpty()) {
            report.append("No users found\n");
            return report.toString();
        }

        // Параллельная обработка пользователей
        Map<User, List<String>> userRolesMap = users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        user -> user,
                        user -> assignmentManager.findByUser(user).stream()
                                .map(a -> a.role().name())
                                .collect(Collectors.toList())
                ));

        Map<User, Integer> userPermissionsMap = users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        user -> user,
                        user -> assignmentManager.getUserPermissions(user).size()
                ));

        String[] headers = {"Username", "Full Name", "Email", "Roles", "Permissions"};
        List<String[]> rows = users.stream()
                .map(user -> new String[]{
                        user.username(),
                        user.fullName(),
                        user.email(),
                        String.join(", ", userRolesMap.get(user)),
                        String.valueOf(userPermissionsMap.get(user))
                })
                .collect(Collectors.toList());

        report.append(FormatUtils.formatTable(headers, rows));
        report.append("\nTotal users: ").append(users.size());

        return report.toString();
    }

    public String generateRoleReport(RoleManager roleManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append(FormatUtils.formatHeader("Role Report"))
                .append("\nGenerated: ")
                .append(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .append("\n\n");

        List<Role> roles = roleManager.findAll(null, null);

        if (roles.isEmpty()) {
            report.append("No roles found\n");
            return report.toString();
        }

        // Параллельная обработка ролей
        Map<Role, Integer> roleUserCountMap = roles.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        role -> role,
                        role -> assignmentManager.findByRole(role).size()
                ));

        Map<Role, Integer> rolePermissionCountMap = roles.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        role -> role,
                        role -> role.getPermissions().size()
                ));

        String[] headers = {"Role Name", "Description", "Permissions", "Users"};
        List<String[]> rows = roles.stream()
                .map(role -> new String[]{
                        role.name(),
                        FormatUtils.truncate(role.description(), 30),
                        String.valueOf(rolePermissionCountMap.get(role)),
                        String.valueOf(roleUserCountMap.get(role))
                })
                .collect(Collectors.toList());

        report.append(FormatUtils.formatTable(headers, rows));
        report.append("\nTotal roles: ").append(roles.size());

        return report.toString();
    }

    public String generatePermissionMatrix(UserManager userManager, AssignmentManager assignmentManager) {
        StringBuilder report = new StringBuilder();

        report.append(FormatUtils.formatHeader("Permission Matrix"))
                .append("\nGenerated: ")
                .append(LocalDateTime.now().format(TIMESTAMP_FORMATTER))
                .append("\n\n");

        List<User> users = userManager.findAll(null, null);

        if (users.isEmpty()) {
            report.append("No users found\n");
            return report.toString();
        }

        // Сбор всех ресурсов параллельно
        Set<String> resources = users.parallelStream()
                .flatMap(user -> assignmentManager.getUserPermissions(user).stream())
                .map(Permission::resource)
                .collect(Collectors.toSet());

        if (resources.isEmpty()) {
            report.append("No permissions found\n");
            return report.toString();
        }

        // Параллельная проверка прав доступа
        Map<User, Set<String>> userResourcesMap = users.parallelStream()
                .collect(Collectors.toConcurrentMap(
                        user -> user,
                        user -> assignmentManager.getUserPermissions(user).stream()
                                .map(Permission::resource)
                                .collect(Collectors.toSet())
                ));

        String[] headers = new String[resources.size() + 1];
        headers[0] = "User";
        List<String> resourceList = new ArrayList<>(resources);
        for (int i = 0; i < resourceList.size(); i++) {
            headers[i + 1] = resourceList.get(i);
        }

        List<String[]> rows = users.stream()
                .map(user -> {
                    String[] row = new String[resources.size() + 1];
                    row[0] = user.username();

                    Set<String> userResources = userResourcesMap.get(user);
                    for (int i = 0; i < resourceList.size(); i++) {
                        row[i + 1] = userResources.contains(resourceList.get(i)) ? "✓" : "✗";
                    }
                    return row;
                })
                .collect(Collectors.toList());

        report.append(FormatUtils.formatTable(headers, rows));

        return report.toString();
    }

    public void exportToFile(String report, String filename) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(report);
        }
    }
}