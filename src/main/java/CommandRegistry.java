import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {
    public static void registerCommands(CommandParser parser, RBACSystem system) {
        parser.registerCommand("user-list", "Список пользователей",
                (scanner, sys) -> {
                    List<User> users = sys.getUserManager().findAll(null, UserSorters.byUsername());

                    if (users.isEmpty()) {
                        System.out.println("Пользователи не найдены");
                        return;
                    }

                    String[] headers = {"Username", "Full Name", "Email"};
                    List<String[]> rows = new ArrayList<>();

                    for (User user : users) {
                        rows.add(new String[]{
                                user.username(),
                                user.fullName(),
                                user.email()
                        });
                    }

                    System.out.println(FormatUtils.formatHeader("Список пользователей"));
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("user-create", "Создать нового пользователя",
                (scanner, sys) -> {
                    String username = ConsoleUtils.promptString(scanner, "Имя пользователя", true);
                    if (!ValidationUtils.isValidUsername(username)) {
                        System.out.println("Неверный формат имени пользователя");
                        return;
                    }

                    String email = ConsoleUtils.promptString(scanner, "Email", true);
                    if (!ValidationUtils.isValidEmail(email)) {
                        System.out.println("Неверный формат email");
                        return;
                    }

                    String fullName = ConsoleUtils.promptString(scanner, "Полное имя", true);
                    try {
                        ValidationUtils.requireNonEmpty(fullName, "Full Name");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Полное имя не может быть пустым");
                        return;
                    }

                    try {
                        User user = User.create(username, fullName, email);
                        sys.getUserManager().add(user);

                        sys.getAuditLog().log("CREATE_USER", sys.getCurrentUser(), username,
                                "Created user: " + fullName);

                        System.out.println("Пользователь '" + username + "' успешно создан");
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("user-view", "Просмотр информации о пользователе",
                (scanner, sys) -> {
                    System.out.println("Введите имя пользователя:");
                    String username = scanner.nextLine().trim();

                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isPresent()) {
                        User user = userOpt.get();
                        System.out.println("\n=== Информация о пользователе ===");
                        System.out.println(user.format());

                        System.out.println("\n=== Назначение роли ===");
                        List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(user);
                        if (assignments.isEmpty()) {
                            System.out.println("Нет назначенных ролей");
                        } else {
                            assignments.forEach(assignment -> System.out.println(assignment.summary()));
                        }

                        System.out.println("\n=== Все права доступа ===");
                        Set<Permission> permissions = sys.getAssignmentManager().getUserPermissions(user);
                        if (permissions.isEmpty()) {
                            System.out.println("Нет прав доступа");
                        } else {
                            permissions.forEach(permission -> System.out.println(permission.format()));
                        }
                        System.out.println("====================================");
                    } else {
                        System.out.println("Пользователь не найден");
                    }
                });

        parser.registerCommand("user-update", "Обновить данные пользователя",
                (scanner, sys) -> {
                    System.out.println("Введите имя пользователя для обновления:");
                    String username = scanner.nextLine().trim();

                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    User user = userOpt.get();

                    System.out.println("Текущие данные:");
                    System.out.println(user.format());

                    System.out.println("\nВведите новые данные (нажмите Enter для оставления текущего значения):");

                    System.out.println("Введите новое полное имя:");
                    String fullName = scanner.nextLine().trim();

                    System.out.println("Введите новый email:");
                    String email = scanner.nextLine().trim();

                    String newFullName = fullName.isEmpty() ? user.fullName() : fullName;
                    String newEmail = email.isEmpty() ? user.email() : email;

                    try {
                        sys.getUserManager().update(username, newFullName, newEmail);
                        System.out.println("Данные пользователя успешно обновлены");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Ошибка обновления: " + e.getMessage());
                    }
                });

        parser.registerCommand("user-delete", "Удалить пользователя",
                (scanner, sys) -> {
                    String username = ConsoleUtils.promptString(scanner, "Имя пользователя", true);

                    var userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь '" + username + "' не найден");
                        return;
                    }

                    if (!ConsoleUtils.promptYesNo(scanner, "Подтвердить удаление?")) {
                        return;
                    }

                    try {
                        sys.getUserManager().remove(userOpt.get());

                        sys.getAuditLog().log("DELETE_USER", sys.getCurrentUser(), username,
                                "Deleted user");

                        System.out.println("Пользователь '" + username + "' успешно удалён");
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("user-search", "Поиск пользователей",
                (scanner, sys) -> {
                    System.out.println("1. По имени пользователя");
                    System.out.println("2. По email");
                    System.out.println("3. По домену email");

                    int choice = ConsoleUtils.promptInt(scanner, "Фильтр", 1, 3);

                    String value = ConsoleUtils.promptString(scanner, "Значение", true);
                    value = ValidationUtils.normalizeString(value);

                    UserFilter filter = null;

                    switch (choice) {
                        case 1:
                            filter = UserFilters.byUsernameContains(value);
                            break;
                        case 2:
                            String finalValue = value;
                            filter = user -> user.email().toLowerCase().contains(finalValue);
                            break;
                        case 3:
                            if (!value.startsWith("@")) {
                                value = "@" + value;
                            }
                            filter = UserFilters.byEmailDomain(value);
                            break;
                    }

                    List<User> users = sys.getUserManager().findAll(filter, UserSorters.byUsername());

                    if (users.isEmpty()) {
                        System.out.println("Пользователи не найдены");
                        return;
                    }

                    String[] headers = {"Username", "Full Name", "Email"};
                    List<String[]> rows = new ArrayList<>();

                    for (User user : users) {
                        rows.add(new String[]{
                                user.username(),
                                user.fullName(),
                                user.email()
                        });
                    }

                    System.out.println(FormatUtils.formatHeader("Результаты поиска"));
                    System.out.println(FormatUtils.formatTable(headers, rows));
                    System.out.println("\nНайдено пользователей: " + users.size());

                    sys.getAuditLog().log("SEARCH_USER", sys.getCurrentUser(), String.valueOf(users.size()),
                            "Searched by filter type " + choice);
                });

        parser.registerCommand("role-list", "Список ролей",
                (scanner, sys) -> {
                    List<Role> roles = sys.getRoleManager().findAll(null, RoleSorters.byName());

                    if (roles.isEmpty()) {
                        System.out.println("Роли не найдены");
                        return;
                    }

                    String[] headers = {"Role Name", "Description", "Permissions"};
                    List<String[]> rows = new ArrayList<>();

                    for (Role role : roles) {
                        rows.add(new String[]{
                                role.name(),
                                FormatUtils.truncate(role.description(), 40),
                                String.valueOf(role.getPermissions().size())
                        });
                    }

                    System.out.println(FormatUtils.formatHeader("Список ролей"));
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("role-create", "Создать новую роль",
                (scanner, sys) -> {
                    String roleName = ConsoleUtils.promptString(scanner, "Название роли", true);

                    try {
                        ValidationUtils.requireNonEmpty(roleName, "Role name");

                        String description = ConsoleUtils.promptString(scanner, "Описание роли", true);
                        ValidationUtils.requireNonEmpty(description, "Description");

                        Role role = new Role(roleName, description);
                        sys.getRoleManager().add(role);

                        sys.getAuditLog().log("CREATE_ROLE", sys.getCurrentUser(), roleName, "Created role");
                        System.out.println("Роль '" + roleName + "' успешно создана");
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-view", "Просмотр роли",
                (scanner, sys) -> {
                    List<Role> roles = sys.getRoleManager().findAll(null, null);
                    if (roles.isEmpty()) {
                        System.out.println("Роли не найдены");
                        return;
                    }

                    Role role = ConsoleUtils.promptChoice(scanner, "Выберите роль:", roles);

                    System.out.println(FormatUtils.formatHeader("Информация о роли"));
                    System.out.println("Название: " + role.name());
                    System.out.println("Описание: " + role.description());
                    System.out.println("Права: " + role.getPermissions().size());
                });

        parser.registerCommand("role-update", "Обновить данные роли",
                (scanner, sys) -> {
                    System.out.println("Введите имя роли для обновления:");
                    String roleName = scanner.nextLine().trim();

                    Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                    if (roleOpt.isEmpty()) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    Role role = roleOpt.get();

                    System.out.println("Текущие данные:");
                    System.out.println(role.format());

                    System.out.println("\nВведите новые данные (нажмите Enter для оставления текущего значения):");

                    System.out.println("Введите новое название роли:");
                    String newName = scanner.nextLine().trim();

                    System.out.println("Введите новое описание роли:");
                    String newDescription = scanner.nextLine().trim();

                    String updatedName = newName.isEmpty() ? role.name() : newName;
                    String updatedDescription = newDescription.isEmpty() ? role.description() : newDescription;

                    try {
                        sys.getRoleManager().remove(role);

                        Role updatedRole = new Role(updatedName, updatedDescription);
                        for (Permission permission : role.getPermissions()) {
                            updatedRole.addPermission(permission);
                        }

                        sys.getRoleManager().add(updatedRole);

                        System.out.println("Роль успешно обновлена");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Ошибка обновления: " + e.getMessage());
                    } catch (Exception e) {
                        System.out.println("Ошибка при обновлении роли: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-delete", "Удалить роль",
                (scanner, sys) -> {
                    System.out.println("Введите имя роли для удаления");
                    String roleName = scanner.nextLine().trim();

                    Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                    if (roleOpt.isEmpty()) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    Role role = roleOpt.get();
                    List<RoleAssignment> assignments = sys.getAssignmentManager().findByRole(role);

                    if (!assignments.isEmpty()) {
                        System.out.println("Роль назначена следующим пользователям:");
                        assignments.forEach(assignment ->
                                System.out.println("- " + assignment.user().username()));

                        System.out.println("Удалить роль? Это приведет к удалению всех назначений (да/нет):");
                        String confirm = scanner.nextLine().trim().toLowerCase(Locale.ROOT);

                        if (!"да".equals(confirm)) {
                            System.out.println("Удаление отменено");
                            return;
                        }
                    }

                    try {
                        sys.getRoleManager().remove(role);
                        System.out.println("Роль успешно удалена");
                    } catch (Exception e) {
                        System.out.println("Ошибка при удалении роли: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-add-permission", "Добавить право к роли",
                (scanner, sys) -> {
                    System.out.println("Введите имя роли:");
                    String roleName = scanner.nextLine().trim();

                    Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                    if (roleOpt.isEmpty()) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    System.out.println("Введите название права:");
                    String permName = scanner.nextLine().trim();

                    System.out.println("Введите ресурс:");
                    String resource = scanner.nextLine().trim();

                    System.out.println("Введите описание права:");
                    String description = scanner.nextLine().trim();

                    try {
                        Permission permission = new Permission(permName, resource, description);
                        sys.getRoleManager().addPermissionToRole(roleName, permission);
                        System.out.println("Право успешно добавлено к роли");
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-remove-permission", "Удалить право из роли",
                (scanner, sys) -> {
                    System.out.println("Введите имя роли:");
                    String roleName = scanner.nextLine().trim();

                    Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                    if(roleOpt.isEmpty()) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    Role role = roleOpt.get();
                    List<Permission> permissions = new ArrayList<>(role.getPermissions());

                    if (permissions.isEmpty()) {
                        System.out.println("Роль не содержит прав доступа");
                        return;
                    }

                    System.out.println("\n=== Права доступа ===");
                    for (int i = 0; i < permissions.size(); i++) {
                        System.out.println(i + 1 + ". " + permissions.get(i).format());
                    }

                    System.out.println("\nВведите номер права для удаления (1 -" + permissions.size() + "):");
                    int index = Integer.parseInt(scanner.nextLine().trim()) - 1;

                    if (index < 0 || index >= permissions.size()) {
                        System.out.println("Неверный номер");
                        return;
                    }

                    try {
                        sys.getRoleManager().removePermissionFromRole(roleName, permissions.get(index));
                        System.out.println("Право успешно удалено из роли");
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-search", "Поиск ролей по фильтрам",
                (scanner, sys) -> {
                    System.out.println("\n=== Фильтры для поиска ===");
                    System.out.println("1. По имени роли (содержит)");
                    System.out.println("2. По наличию конкретного права");
                    System.out.println("3. По минимальному количеству прав");
                    System.out.println("0. Применить все фильтры");

                    RoleFilter filter = null;

                    while (true) {
                        System.out.println("\nВыберите фильтр (0 для применения):");
                        int choice = Integer.parseInt(scanner.nextLine().trim());

                        if (choice == 0) break;

                        RoleFilter currentFilter = null;

                        switch (choice) {
                            case 1:
                                System.out.println("Введите имя роли (содержит):");
                                String name = scanner.nextLine().trim();
                                currentFilter = RoleFilters.byNameContains(name);
                                break;
                            case 2:
                                System.out.println("Введите название права:");
                                String permName = scanner.nextLine().trim();

                                System.out.println("Введите ресурс:");
                                String resource = scanner.nextLine().trim();

                                currentFilter = RoleFilters.hasPermission(permName, resource);
                                break;
                            case 3:
                                System.out.println("Введите минимальное количество прав:");
                                int minPermissions = Integer.parseInt(scanner.nextLine().trim());
                                currentFilter = RoleFilters.hasAtLeastNPermissions(minPermissions);
                                break;
                            default:
                                System.out.println("Неверный выбор");
                        }

                        if (currentFilter != null) {
                            filter = (filter == null) ? currentFilter : filter.and(currentFilter);
                        }
                    }

                    List<Role> roles = sys.getRoleManager().findAll(filter, RoleSorters.byName());

                    if (roles.isEmpty()) {
                        System.out.println("Роли не найдены");
                        return;
                    }

                    System.out.println("\n=== Результаты поиска ===");
                    System.out.println("Всего найдено: " + roles.size());
                    for (Role role : roles) {
                        System.out.println(role.format());
                        System.out.println("============================================================");
                    }
                });

        parser.registerCommand("assign-role", "Назначить роль пользователю",
                (scanner, sys) -> {
                    String username = ConsoleUtils.promptString(scanner, "Имя пользователя", true);
                    var userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь не найден");
                        return;
                    }
                    User user = userOpt.get();

                    List<Role> roles = sys.getRoleManager().findAll(null, null);
                    Role role = ConsoleUtils.promptChoice(scanner, "Выберите роль:", roles);

                    System.out.println("1. Постоянное");
                    System.out.println("2. Временное");
                    int type = ConsoleUtils.promptInt(scanner, "Тип назначения", 1, 2);

                    String expiresAt = null;
                    boolean autoRenew = false;

                    if (type == 2) {
                        expiresAt = ConsoleUtils.promptString(scanner, "Дата истечения (yyyy-MM-dd HH:mm)", true);

                        if (!ValidationUtils.isValidDate(expiresAt)) {
                            System.out.println("Неверный формат даты. Используйте: yyyy-MM-dd HH:mm");
                            return;
                        }

                        String today = DateUtils.getCurrentDate();
                        if (!DateUtils.isAfter(expiresAt.substring(0, 10), today)) {
                            System.out.println("Дата истечения должна быть в будущем");
                            return;
                        }

                        autoRenew = ConsoleUtils.promptYesNo(scanner, "Автоматическое продление?");
                    }

                    String reason = ConsoleUtils.promptString(scanner, "Причина назначения", true);
                    AssignmentMetadata metadata = AssignmentMetadata.now(sys.getCurrentUser(), reason);
                    RoleAssignment assignment = (type == 1) ?
                            new PermanentAssignment(user, role, metadata) :
                            new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);

                    sys.getAssignmentManager().add(assignment);
                    sys.getAuditLog().log("ASSIGN_ROLE", sys.getCurrentUser(), username, "Assigned role " + role.name());
                    System.out.println("Роль '" + role.name() + "' успешно назначена");
                });

        parser.registerCommand("revoke-role", "Отозвать роль у пользователя",
                (scanner, sys) -> {
                    System.out.println("Введите имя пользователя:");
                    String username = scanner.nextLine().trim();

                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    User user = userOpt.get();
                    List<RoleAssignment> activeAssignments = sys.getAssignmentManager()
                            .getActiveAssignments().stream()
                            .filter(assignment -> assignment.user().equals(user))
                            .toList();

                    if (activeAssignments.isEmpty()) {
                        System.out.println("У пользователя нет активных назначений");
                        return;
                    }

                    System.out.println("\n=== Активные назначения ===");
                    for (int i = 0; i < activeAssignments.size(); i++) {
                        System.out.println(i + 1 + ". " + activeAssignments.get(i).summary());
                    }

                    System.out.println("\nВведите номер назначения для отзыва:");
                    int index = Integer.parseInt(scanner.nextLine().trim())- 1;

                    if (index < 0 || index >= activeAssignments.size()) {
                        System.out.println("Неверный номер");
                        return;
                    }

                    RoleAssignment assignment = activeAssignments.get(index);

                    if (assignment instanceof PermanentAssignment) {
                        ((PermanentAssignment) assignment).revoke();
                        System.out.println("Постоянное назначение успешно отозвано");
                    } else {
                        System.out.println("Нельзя отозвать временное назначение. Используйте команду 'assignment-extend' для продления");
                    }
                });

        parser.registerCommand("assignment-list", "Список назначений",
                (scanner, sys) -> {
                    List<RoleAssignment> assignments = sys.getAssignmentManager().findAll(null, null);

                    if (assignments.isEmpty()) {
                        System.out.println("Назначения не найдены");
                        return;
                    }

                    String[] headers = {"User", "Role", "Type", "Status"};
                    List<String[]> rows = new ArrayList<>();

                    for (RoleAssignment assignment : assignments) {
                        rows.add(new String[]{
                                assignment.user().username(),
                                assignment.role().name(),
                                assignment.assignmentType(),
                                assignment.isActive() ? "Active" : "Inactive"
                        });
                    }

                    System.out.println(FormatUtils.formatHeader("Список назначений"));
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("assignment-list-user", "Назначение конкретного пользователя",
                (scanner, sys) -> {
                    System.out.println("Введите имя пользователя:");
                    String username = scanner.nextLine().trim();

                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(userOpt.get());

                    if (assignments.isEmpty()) {
                        System.out.println("У пользователя нет назначений");
                        return;
                    }

                    System.out.println("\n=== Назначение для " + username + " ===");
                    for (RoleAssignment assignment : assignments) {
                        System.out.println(assignment.summary());
                        System.out.println("============================================================");
                    }
                });

        parser.registerCommand("assignment-list-role", "Пользователи с конкретной ролью",
                (scanner, sys) -> {
                    System.out.println("Введите имя роли:");
                    String roleName = scanner.nextLine().trim();

                    Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                    if (roleOpt.isEmpty()) {
                        System.out.println("Роль не найдена");
                        return;
                    }

                    List<RoleAssignment> assignments = sys.getAssignmentManager().findByRole(roleOpt.get());

                    if (assignments.isEmpty()) {
                        System.out.println("Нет пользователей с этой ролью");
                        return;
                    }

                    System.out.println("\n=== Пользователи с ролью " + roleName + " ===");
                    for (RoleAssignment assignment : assignments) {
                        System.out.println(assignment.user().format());
                        System.out.println("============================================================");
                    }
                });

        parser.registerCommand("assignment-active", "Активные назначения",
                (scanner, sys) -> {
                    List<RoleAssignment> assignments = sys.getAssignmentManager().getActiveAssignments();

                    System.out.println("\n=== Активные назначения ===");
                    System.out.printf("%-15s %-15s %-10s %-20s\n", "Пользователь", "Роль", "Тип", "Дата назначения");
                    System.out.println("============================================================");

                    for (RoleAssignment assignment : assignments) {
                        System.out.printf("%-15s %-15s %-10s %-20s\n",
                                assignment.user().username(),
                                assignment.role().name(),
                                assignment.assignmentType(),
                                assignment.metadata().assignedAt());
                    }
                    System.out.println("============================================================");
                });

        parser.registerCommand("assignment-expired", "Истекшие назначения",
                (scanner, sys) -> {
                    List<RoleAssignment> assignments = sys.getAssignmentManager().getExpiredAssignments();

                    System.out.println("\n=== Истекшие назначения ===");
                    System.out.printf("%-15s %-15s %-10s %-20s\n", "Пользователь", "Роль", "Тип", "Дата истечения");
                    System.out.println("============================================================");

                    for (RoleAssignment assignment : assignments) {
                        if (assignment instanceof TemporaryAssignment temp) {
                            System.out.printf("%-15s %-15s %-10s %-20s\n",
                                    assignment.user().username(),
                                    assignment.role().name(),
                                    assignment.assignmentType(),
                                    temp.getExpiresAt());
                        }
                    }
                    System.out.println("============================================================");
                });

        parser.registerCommand("assignment-extend", "Продлить временное назначение",
                (scanner, sys) -> {
                    String username = ConsoleUtils.promptString(scanner, "Имя пользователя", true);
                    var userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    List<RoleAssignment> tempAssignments = sys.getAssignmentManager().findByUser(userOpt.get()).stream()
                            .filter(a -> a instanceof TemporaryAssignment && a.isActive())
                            .collect(Collectors.toList());

                    if (tempAssignments.isEmpty()) {
                        System.out.println("Нет активных временных назначений");
                        return;
                    }

                    RoleAssignment assignment = ConsoleUtils.promptChoice(scanner, "Выберите назначение:", tempAssignments);
                    TemporaryAssignment temp = (TemporaryAssignment) assignment;

                    String newExpiry = ConsoleUtils.promptString(scanner, "Новая дата истечения (yyyy-MM-dd HH:mm)", true);

                    if (!ValidationUtils.isValidDate(newExpiry)) {
                        System.out.println("Неверный формат даты");
                        return;
                    }

                    if (DateUtils.isBefore(newExpiry, temp.expiresAt())) {
                        System.out.println("Новая дата должна быть позже текущей (" + temp.expiresAt() + ")");
                        return;
                    }

                    System.out.println("Назначение продлено до " + newExpiry);
                    sys.getAuditLog().log("EXTEND_ASSIGNMENT", sys.getCurrentUser(), username, "Extended to " + newExpiry);
                });

        parser.registerCommand("assignment-search", "Поиск назначений",
                (scanner, sys) -> {
                    System.out.println("5. Истекающие до даты");
                    int choice = ConsoleUtils.promptInt(scanner, "Фильтр", 5, 5);

                    String date = ConsoleUtils.promptString(scanner, "Дата (yyyy-MM-dd)", true);

                    if (!ValidationUtils.isValidDate(date)) {
                        System.out.println("Неверный формат даты");
                        return;
                    }

                    List<RoleAssignment> results = sys.getAssignmentManager().findAll(null, null).stream()
                            .filter(a -> a instanceof TemporaryAssignment)
                            .filter(a -> {
                                String expires = ((TemporaryAssignment) a).expiresAt().substring(0, 10);
                                return DateUtils.isBefore(expires, date);
                            })
                            .toList();

                    if (results.isEmpty()) {
                        System.out.println("Назначения не найдены");
                        return;
                    }

                    String[] headers = {"User", "Role", "Expires At"};
                    List<String[]> rows = new ArrayList<>();
                    for (RoleAssignment a : results) {
                        TemporaryAssignment temp = (TemporaryAssignment) a;
                        rows.add(new String[]{
                                a.user().username(),
                                a.role().name(),
                                temp.expiresAt()
                        });
                    }

                    System.out.println(FormatUtils.formatHeader("Истекающие назначения"));
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("permissions-user", "Все права конкретного пользователя",
                (scanner, sys) -> {
                    System.out.println("Введите имя пользователя:");
                    String username = scanner.nextLine().trim();

                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("У пользователя нет прав доступа");
                        return;
                    }

                    User user = userOpt.get();
                    Set<Permission> permissions = sys.getAssignmentManager().getUserPermissions(user);

                    Map<String, List<Permission>> grouped = permissions.stream()
                            .collect(Collectors.groupingBy(Permission::resource));

                    System.out.println("\n=== Права пользователя" + username + " ===");
                    for (Map.Entry<String, List<Permission>> entry : grouped.entrySet()) {
                        System.out.println("\nРесурс: " + entry.getKey());
                        entry.getValue().forEach(perm ->
                                System.out.println("- " + perm.name() + ": " + perm.description()));
                    }
                });

        parser.registerCommand("permissions-check", "Проверить право пользователя",
                (scanner, sys) -> {
                    System.out.println("Введите имя пользователя:");
                    String username = scanner.nextLine().trim();

                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    System.out.println("Введите название права:");
                    String permName = scanner.nextLine().trim();

                    System.out.println("Введите ресурс:");
                    String resource = scanner.nextLine().trim();

                    boolean hasPermission = sys.getAssignmentManager().userHasPermission(userOpt.get(), permName, resource);

                    System.out.println("\n=== Проверка прав ===");
                    System.out.println("Пользователь: " + username);
                    System.out.println("Право: " + permName + "для ресурса " + resource);
                    System.out.println("Имеет право: " + (hasPermission ? "Да" : "Нет"));

                    if (hasPermission) {
                        System.out.println("\n=== Источник прав ===");
                        List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(userOpt.get());
                        for (RoleAssignment assignment : assignments) {
                            if (assignment.role().hasPermission(permName, resource)) {
                                System.out.println("- Из роли: " + assignment.role().name());
                            }
                        }
                    }
                });

        parser.registerCommand("audit-log", "Просмотр лога аудита",
                (scanner, sys) -> {
                    List<AuditLog.AuditEntry> entries = sys.getAuditLog().getAll();

                    if (entries.isEmpty()) {
                        System.out.println("Лог пуст");
                        return;
                    }

                    String[] headers = {"Timestamp", "Action", "Performer", "Target"};
                    List<String[]> rows = new ArrayList<>();

                    for (AuditLog.AuditEntry entry : entries) {
                        rows.add(new String[]{
                                entry.timestamp(),
                                entry.action(),
                                entry.performer(),
                                entry.target()
                        });
                    }

                    System.out.println(FormatUtils.formatHeader("Audit Log"));
                    System.out.println(FormatUtils.formatTable(headers, rows));
                });

        parser.registerCommand("help", "Справка по командам",
                (scanner, sys) -> {
                    String helpText = "user-list     — список пользователей\n" +
                            "user-create   — создать пользователя\n" +
                            "user-delete   — удалить пользователя\n" +
                            "role-list     — список ролей\n" +
                            "role-view     — просмотр роли\n" +
                            "role-create   — создать роль\n" +
                            "role-delete   — удалить роль\n" +
                            "assign-role   — назначить роль\n" +
                            "revoke-role   — отозвать роль\n" +
                            "report-users  — отчёт по пользователям\n" +
                            "report-roles  — отчёт по ролям\n" +
                            "report-matrix — матрица прав\n" +
                            "audit-log     — просмотр лога аудита";

                    System.out.println(FormatUtils.formatBox("Доступные команды"));
                    System.out.println(helpText);
                });

        parser.registerCommand("stats", "Статистика системы",
                (scanner, sys) -> {
                    System.out.println(sys.generateStatistics());
                });

        parser.registerCommand("clear", "Очистить экран",
                (scanner, sys) -> {
                    System.out.println("\033[H\033[2J");
                    System.out.flush();
                    System.out.println("Экран очищен");
                });

        parser.registerCommand("exit", "Выход из программы",
                (scanner, sys) -> {
                    System.out.println("Выход из системы. До свидания");
                    System.exit(0);
                });

        parser.registerCommand("save", "Сохранить данные в файл",
                (scanner, sys) -> {
                    System.out.println("Введите имя файла для сохранения (например, data.txt):");
                    String filename = scanner.nextLine().trim();

                    if (filename.isEmpty()) {
                        System.out.println("Ошибка: имя файла не может быть пустым");
                        return;
                    }

                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
                        writer.write("# RBAC DATA FILE");
                        writer.newLine();
                        writer.write("#VERSION: 1.0");
                        writer.newLine();
                        writer.write("# EXPORTED AT: " + LocalDateTime.now().format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                        writer.newLine();
                        writer.newLine();

                        writer.write("[USERS]");
                        writer.newLine();
                        List<User> users = sys.getUserManager().findAll(null, UserSorters.byUsername());
                        for (User user : users) {
                            writer.write(escape(user.username()) + "|" +
                                    escape(user.fullName()) + "|" +
                                    escape(user.email()));
                            writer.newLine();
                        }
                        writer.newLine();

                        writer.write("[ROLES]");
                        writer.newLine();
                        List<Role> roles = sys.getRoleManager().findAll(null, RoleSorters.byName());
                        for (Role role : roles) {
                            writer.write(escape(role.id()) + "|" +
                                    escape(role.name()) + "|" +
                                    escape(role.description()));
                            writer.newLine();
                        }
                        writer.newLine();

                        writer.write("[PERMISSIONS]");
                        writer.newLine();
                        for (Role role : roles) {
                            for (Permission permission : role.getPermissions()) {
                                writer.write(escape(role.id()) + "|" +
                                        escape(permission.name()) + "|" +
                                        escape(permission.resource()) + "|" +
                                        escape(permission.description()));
                                writer.newLine();
                            }
                        }
                        writer.newLine();

                        writer.write("[ASSIGNMENTS]");
                        writer.newLine();
                        List<RoleAssignment> assignments = sys.getAssignmentManager()
                                .findAll(null, AssignmentSorters.byAssignmentDate());
                        for(RoleAssignment assignment : assignments) {
                            StringBuilder line = new StringBuilder();
                            line.append(escape(assignment.assignmentId())).append("|")
                                    .append(escape(assignment.user().username())).append("|")
                                    .append(escape(assignment.role().id())).append("|")
                                    .append(escape(assignment.assignmentType())).append("|")
                                    .append(escape(assignment.metadata().assignedAt())).append("|")
                                    .append(escape(assignment.metadata().assignedBy())).append("|")
                                    .append(escape(assignment.metadata().reason())).append("|")
                                    .append(assignment.isActive());

                            if (assignment instanceof TemporaryAssignment temp) {
                                line.append("|")
                                        .append(escape(temp.getExpiresAt())).append("|")
                                        .append(temp.autoRenew);
                            }

                            writer.write(line.toString());
                            writer.newLine();
                        }

                        System.out.println("Данные успешно сохранены в файл: " + filename);
                        System.out.println("Сохранено: " + users.size() + " пользователей, " +
                                roles.size() + " ролей, " + assignments.size() + " назначений");

                    } catch (IOException e) {
                        System.out.println("Ошибка при сохранении файла: " + e.getMessage());
                    }
                });

        parser.registerCommand("load", "Загрузить данные из файла",
                (scanner, sys) -> {
                    System.out.println("Введите имя файла для загрузки (например, data.txt):");
                    String filename = scanner.nextLine().trim();

                    if (filename.isEmpty()) {
                        System.out.println("Ошибка: имя файла не может быть пустым");
                        return;
                    }

                    File file = new File(filename);
                    if (!file.exists()) {
                        System.out.println("Ошибка: файл '" + filename + "' не найден");
                        return;
                    }

                    System.out.println("Внимание: текущие данные будут удалены");
                    System.out.println("Подтвердите загрузку (введите 'да' для подтверждения):");
                    String confirm = scanner.nextLine().trim();
                    if (!"да".equals(confirm)) {
                        System.out.println("Загрузка отменена");
                        return;
                    }

                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        sys.getUserManager().clear();
                        sys.getRoleManager().clear();
                        sys.getAssignmentManager().clear();

                        String line;
                        String section = null;
                        Map<String, User> loadedUsers = new HashMap<>();
                        Map<String, Role> loadedRoles = new HashMap<>();

                        while ((line = reader.readLine()) != null) {
                            line = line.trim();

                            if (line.isEmpty() || line.startsWith("#")) {
                                continue;
                            }

                            if (line.startsWith("[") && line.endsWith("]")) {
                                section = line.substring(1, line.length() - 1);
                                continue;
                            }

                            if ("USERS".equals(section)) {
                                String[] parts = line.split("\\|", 3);
                                if (parts.length != 3) {
                                    continue;
                                }

                                try {
                                    User user = User.create(
                                            unescape(parts[0]),
                                            unescape(parts[1]),
                                            unescape(parts[2])
                                    );
                                    sys.getUserManager().add(user);
                                    loadedUsers.put(user.username(), user);
                                } catch (Exception e) {
                                    System.out.println("Предупреждение: пропущен некорректный пользователь: " + line);
                                }
                            } else if ("ROLES".equals(section)) {
                                String[] parts = line.split("\\|", 3);
                                if (parts.length != 3) {
                                    continue;
                                }

                                try {
                                    Role role = new Role(unescape(parts[1]), unescape(parts[2]));
                                    Field idField = Role.class.getDeclaredField("id");
                                    idField.setAccessible(true);
                                    idField.set(role, unescape(parts[0]));

                                    sys.getRoleManager().add(role);
                                    loadedRoles.put(role.id(), role);
                                } catch (Exception e) {
                                    System.out.println("Предупреждение: пропущена некорректная роль: " + line);
                                }
                            } else if ("PERMISSIONS".equals(section)) {
                                String[] parts = line.split("\\|", 4);
                                if (parts.length != 4) {
                                    continue;
                                }

                                String roleId = unescape(parts[0]);
                                Role role = loadedRoles.get(roleId);
                                if (role != null) {
                                    try {
                                        Permission permission = new Permission(
                                                unescape(parts[1]),
                                                unescape(parts[2]),
                                                unescape(parts[3])
                                        );
                                        role.addPermission(permission);
                                    } catch (Exception e) {
                                        System.out.println("Предупреждение: пропущено некорректное право:" + line);
                                    }
                                }
                            } else if ("ASSIGNMENT".equals(section)) {
                                String[] parts = line.split("\\|");
                                if (parts.length < 8) {
                                    continue;
                                }

                                try {
                                    String assignmentId = unescape(parts[0]);
                                    String username = unescape(parts[1]);
                                    String roleId = unescape(parts[2]);
                                    String type = unescape(parts[3]);
                                    String assignedAt = unescape(parts[4]);
                                    String assignedBy = unescape(parts[5]);
                                    String reason = unescape(parts[6]);
                                    boolean isActive = Boolean.parseBoolean(parts[7]);

                                    User user = loadedUsers.get(username);
                                    Role role = loadedRoles.get(roleId);

                                    if (user == null || role == null) {
                                        System.out.println("Предупреждение: пропущено назначение для несуществующего пользователя/роли: ");
                                        continue;
                                    }

                                    AssignmentMetadata metadata = new AssignmentMetadata(assignedBy, assignedAt, reason);

                                    RoleAssignment assignment;
                                    if ("PERMANENT".equals(type)) {
                                        assignment = new PermanentAssignment(user, role, metadata);
                                        if (!isActive) {
                                            ((PermanentAssignment) assignment).revoke();
                                        }
                                    } else if ("TEMPORARY".equals(type)) {
                                        if (parts.length < 10) {
                                            continue;
                                        }
                                        String expiresAt = unescape(parts[8]);
                                        boolean autoRenew = Boolean.parseBoolean(parts[9]);

                                        assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                                    } else {
                                        continue;
                                    }

                                    Field idField = AbstractRoleAssignment.class.getDeclaredField("assignmentId");
                                    idField.setAccessible(true);
                                    idField.set(assignment, assignmentId);

                                    sys.getAssignmentManager().add(assignment);

                                } catch (Exception e) {
                                    System.out.println("Предупреждение: пропущено некорректное назначение: " + line);
                                }
                            }
                        }

                        System.out.println("Данные успешно загружены из файла: " + filename);
                        System.out.println("Загружено: " + sys.getUserManager().count() + " пользователей, " +
                                sys.getRoleManager().count() + " ролей, " +
                                sys.getAssignmentManager().count() + " назначений");
                    } catch (IOException e) {
                        System.out.println("Ошибка при чтении файла: " + e.getMessage());
                    } catch (IllegalArgumentException e) {
                        System.out.println("Ошибка при восстановлении идентификаторов: " + e.getMessage());
                    }
                });

        parser.registerCommand("report-users", "Отчёт по пользователям",
                (scanner, sys) -> {
                    String report = sys.getReportGenerator().generateUserReport(
                            sys.getUserManager(),
                            sys.getAssignmentManager()
                    );
                    System.out.println(report);

                    if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                        String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
                        try {
                            sys.getReportGenerator().exportToFile(report, filename);
                            System.out.println("Отчёт сохранён в файл: " + filename);
                        } catch (Exception e) {
                            System.out.println("Ошибка сохранения: " + e.getMessage());
                        }
                    }
                });

        parser.registerCommand("report-roles", "Отчёт по ролям",
                (scanner, sys) -> {
                    String report = sys.getReportGenerator().generateRoleReport(
                            sys.getRoleManager(),
                            sys.getAssignmentManager()
                    );
                    System.out.println(report);

                    if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                        String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
                        try {
                            sys.getReportGenerator().exportToFile(report, filename);
                            System.out.println("Отчёт сохранён в файл: " + filename);
                        } catch (Exception e) {
                            System.out.println("Ошибка сохранения: " + e.getMessage());
                        }
                    }
                });

        parser.registerCommand("report-matrix", "Матрица прав доступа",
                (scanner, sys) -> {
                    String report = sys.getReportGenerator().generatePermissionMatrix(
                            sys.getUserManager(),
                            sys.getAssignmentManager()
                    );
                    System.out.println(report);

                    if (ConsoleUtils.promptYesNo(scanner, "Сохранить отчёт в файл?")) {
                        String filename = ConsoleUtils.promptString(scanner, "Имя файла", true);
                        try {
                            sys.getReportGenerator().exportToFile(report, filename);
                            System.out.println("Отчёт сохранён в файл: " + filename);
                        } catch (Exception e) {
                            System.out.println("Ошибка сохранения: " + e.getMessage());
                        }
                    }
                });
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static String unescape(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return value.replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\|", "|")
                .replace("\\\\", "\\");
    }
}