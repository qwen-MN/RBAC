import java.io.*;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class CommandRegistry {
    public static void registerCommands(CommandParser parser, RBACSystem system) {
        parser.registerCommand("user-list", "Вывести список всех пользователей",
                (scanner, sys) -> {
                    List<User> users = sys.getUserManager().findAll(null, null);
                    System.out.println("\n=== Список пользователей ===");
                    users.forEach(user -> System.out.println(user.format()));
                    System.out.println("====================================");
                });

        parser.registerCommand("user-create", "Создать нового пользователя",
                (scanner, sys) -> {
                    System.out.println("Введите имя пользователя:");
                    String username = scanner.nextLine().trim();

                    System.out.println("Введите полное имя:");
                    String fullName = scanner.nextLine().trim();

                    System.out.println("Введите email:");
                    String email = scanner.nextLine().trim();

                    try {
                        User user = User.create(username, fullName, email);
                        sys.getUserManager().add(user);
                        System.out.println("Пользователь успешно создан");
                    } catch (IllegalArgumentException e) {
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
                    System.out.println("Введите имя пользователя для удаления:");
                    String username = scanner.nextLine().trim();

                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    System.out.println("Вы уверены, что хотите удалить пользователя " + username + "?");
                    System.out.println("Это действие удалит все назначения пользователя и не может быть отменено.");
                    System.out.println("Введите 'да' для подтверждения:");
                    String confirm = scanner.nextLine().trim().toLowerCase();

                    if ("да".equals(confirm)) {
                        try {
                            List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(userOpt.get());
                            for (RoleAssignment assignment : assignments) {
                                sys.getAssignmentManager().remove(assignment);
                            }

                            sys.getUserManager().remove(userOpt.get());
                            System.out.println("Пользователь успешно удален");
                        } catch (Exception e) {
                            System.out.println("Ошибка при удалении пользователя: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Удаление отменено");
                    }
                });

        parser.registerCommand("user-search", "Поиск пользователей по фильтрам",
                (scanner, sys) -> {
                    System.out.println("\n=== Фильтры для поиска ===");
                    System.out.println("1. По имени пользователя");
                    System.out.println("2. По email");
                    System.out.println("3. По домену email");
                    System.out.println("4. По полному имени");
                    System.out.println("0. Применить все фильтры");

                    UserFilter filter = null;

                    while (true) {
                        System.out.println("\nВыберите фильтр (0 для применения):");
                        int choice = Integer.parseInt(scanner.nextLine().trim());

                        if (choice == 0) break;

                        UserFilter currentFilter = null;

                        switch (choice) {
                            case 1:
                                System.out.println("Введите имя пользователя (содержит):");
                                String username = scanner.nextLine().trim();
                                currentFilter = UserFilters.byUsernameContains(username);
                                break;
                            case 2:
                                System.out.println("Введите email (точное совпадение):");
                                String email = scanner.nextLine().trim();
                                currentFilter = UserFilters.byEmail(email);
                                break;
                            case 3:
                                System.out.println("Введите домен email (например, @company.com):");
                                String domain = scanner.nextLine().trim();
                                currentFilter = UserFilters.byEmailDomain(domain);
                                break;
                            case 4:
                                System.out.println("Введите полное имя (содержит):");
                                String fullName = scanner.nextLine().trim();
                                currentFilter = UserFilters.byFullNameContains(fullName);
                                break;
                            default:
                                System.out.println("Неверный выбор");
                        }

                        if (currentFilter != null) {
                            filter = (filter == null) ? currentFilter : filter.and(currentFilter);
                        }
                    }

                    List<User> users = sys.getUserManager().findAll(filter, UserSorters.byUsername());

                    if (users.isEmpty()) {
                        System.out.println("Пользователи не найдены");
                        return;
                    }

                    System.out.println("\n=== Результаты поиска ===");
                    System.out.println("Всего найдено: " + users.size());
                    for (User user : users) {
                        System.out.println(user.format());
                        System.out.println("============================================================");
                    }
                });

        parser.registerCommand("role-list", "Вывести список всех ролей",
                (scanner, sys) -> {
                    List<Role> roles = sys.getRoleManager().findAll(null, null);
                    System.out.println("\n=== Список ролей ===");
                    roles.forEach(role -> System.out.println(role.format()));
                    System.out.println("====================================");
                });

        parser.registerCommand("role-create", "Создать новую роль",
                (scanner, sys) -> {
                    System.out.println("Введите название роли:");
                    String name = scanner.nextLine().trim();

                    System.out.println("Введите описание роли:");
                    String description = scanner.nextLine().trim();

                    try {
                        Role role = new Role(name, description);
                        sys.getRoleManager().add(role);

                        System.out.println("Добавить права доступа к роли? (да/нет)");
                        String answer = scanner.nextLine().trim().toLowerCase(Locale.ROOT);

                        if ("да".equals(answer)) {
                            while (true) {
                                System.out.println("Введите название права (или 'exit' для завершения):");
                                String permName = scanner.nextLine().trim();

                                if ("exit".equalsIgnoreCase(permName)) {
                                    break;
                                }
                                System.out.println("Введите ресурс:");
                                String resource = scanner.nextLine().trim();

                                System.out.println("Введите описание права:");
                                String descriptionPerm = scanner.nextLine().trim();

                                Permission permission = new Permission(permName, resource, descriptionPerm);
                                sys.getRoleManager().addPermissionToRole(name, permission);
                            }
                        }

                        System.out.println("Роль успешно создана");
                    } catch (IllegalArgumentException e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("role-view", "Просмотр информации о роли",
                (scanner, sys) -> {
                    System.out.println("Введите имя роли:");
                    String roleName = scanner.nextLine().trim();

                    Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                    if (roleOpt.isPresent()) {
                        Role role = roleOpt.get();
                        System.out.println("\n=== Информация о роли ===");
                        System.out.println(role.format());

                        System.out.println("\n=== Права доступа ===");
                        role.getPermissions().forEach(permission ->
                                System.out.println(permission.format()));

                        System.out.println("\n=== Назначенные пользователи ===");
                        List<RoleAssignment> assignments = sys.getAssignmentManager().findByRole(role);
                        if (assignments.isEmpty()) {
                            System.out.println("Нет назначенных пользователей");
                        } else {
                            assignments.forEach(assignment ->
                                    System.out.println(assignment.summary()));
                        }

                        System.out.println("====================================");
                    } else {
                        System.out.println("Роль не найдена");
                    }
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
                    System.out.println("Введите имя пользователя:");
                    String username = scanner.nextLine().trim();

                    Optional<User> userOpt = sys.getUserManager().findByUsername(username);
                    if (userOpt.isEmpty()) {
                        System.out.println("Пользователь не найден");
                        return;
                    }

                    User user = userOpt.get();

                    System.out.println("\n=== Доступные роли ===");
                    List<Role> roles = sys.getRoleManager().findAll(null, null);
                    for (int i = 0; i < roles.size(); i++) {
                        System.out.println(i + 1 + ". " + roles.get(i).name());
                    }

                    System.out.println("\nВведите номер роли для назначения:");
                    int roleIndex = Integer.parseInt(scanner.nextLine().trim()) - 1;

                    if (roleIndex < 0 || roleIndex >= roles.size()) {
                        System.out.println("Неверный номер");
                        return;
                    }

                    Role role = roles.get(roleIndex);

                    System.out.println("Выберите тип назначения:");
                    System.out.println("1. Постоянное");
                    System.out.println("2. Временное");
                    int type = Integer.parseInt(scanner.nextLine().trim());

                    System.out.println("Введите причину назначения:");
                    String reason = scanner.nextLine().trim();

                    AssignmentMetadata metadata = AssignmentMetadata.now(
                            sys.getCurrentUser(),
                            reason
                    );

                    if (type == 1) {
                        PermanentAssignment assignment = new PermanentAssignment(user, role, metadata);
                        sys.getAssignmentManager().add(assignment);
                        System.out.println("Роль успешно назначена");
                    } else {
                        System.out.println("Введите дату истечения (yyyy-MM-dd HH:mm):");
                        String expiresAt = scanner.nextLine().trim();

                        System.out.println("Автоматическое продление? (да/нет):");
                        boolean autoRenew = "да".equalsIgnoreCase(scanner.nextLine().trim());

                        TemporaryAssignment assignment = new TemporaryAssignment(user, role, metadata, expiresAt, autoRenew);
                        sys.getAssignmentManager().add(assignment);
                        System.out.println("Временное назначение успешно создано");
                    }
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

                    System.out.println("\n=== Все назначения ===");
                    System.out.printf("%-15s %-15s %-10s %-8s %-20s\n", "Пользователь", "Роль", "Тип", "Статус", "Дата назначения");
                    System.out.println("============================================================");

                    for (RoleAssignment assignment : assignments) {
                        System.out.printf("%-15s %-15s %-10s %-8s %-20s\n",
                                assignment.user().username(),
                                assignment.role().name(),
                                assignment.assignmentType(),
                                assignment.isActive() ? "Активно" : "Неактивно",
                                assignment.metadata().assignedAt());
                    }
                    System.out.println("============================================================");
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
                    System.out.println("Введите ID назначения или имя пользователя");
                    String input = scanner.nextLine().trim();

                    RoleAssignment assignment;

                    Optional<RoleAssignment> assignmentOpt = sys.getAssignmentManager().findById(input);
                    if (assignmentOpt.isPresent()) {
                        assignment = assignmentOpt.get();
                    } else {
                        Optional<User> userOpt = sys.getUserManager().findByUsername(input);
                        if (userOpt.isEmpty()) {
                            System.out.println("Пользователь не найден");
                            return;
                        }

                        System.out.println("Введите имя роли");
                        String roleName = scanner.nextLine().trim();

                        Optional<Role> roleOpt = sys.getRoleManager().findByName(roleName);
                        if (roleOpt.isEmpty()) {
                            System.out.println("Роль не найдена");
                            return;
                        }

                        List<RoleAssignment> assignments = sys.getAssignmentManager().findByUser(userOpt.get()).stream()
                                .filter(a -> a.role().equals(roleOpt.get()))
                                .toList();

                        if (assignments.isEmpty()) {
                            System.out.println("Назначение не найдено");
                            return;
                        }

                        if (assignments.size() > 1) {
                            System.out.println("Найдено несколько назначений. Выберите:");
                            for (int i = 0; i < assignments.size(); i++) {
                                System.out.println(i + 1 + ". " + assignments.get(i).summary());
                            }

                            int index = Integer.parseInt(scanner.nextLine().trim()) - 1;
                            assignment = assignments.get(index);
                        } else {
                            assignment = assignments.get(0);
                        }
                    }

                    if (!(assignment instanceof TemporaryAssignment)) {
                        System.out.println("Это не временное назначение");
                        return;
                    }

                    System.out.println("Введите новую дату истечения (yyyy-MM-dd HH:mm):");
                    String newExpiry = scanner.nextLine().trim();

                    try {
                        sys.getAssignmentManager().extendTemporaryAssignment(
                                assignment.assignmentId(),
                                newExpiry
                        );
                        System.out.println("Назначение успешно продлено");
                    } catch (Exception e) {
                        System.out.println("Ошибка: " + e.getMessage());
                    }
                });

        parser.registerCommand("assignment-search", "Поиск назначений по фильтрам",
                (scanner, sys) -> {
                    System.out.println("""
                            === Фильтры для поиска ===
                            1. По пользователю
                            2. По роли
                            3. По типу
                            4. По статусу
                            5. Назначенные после даты
                            6. Истекающие до даты
                            0. Применить все фильтры
                            """);

                    AssignmentFilter filter = null;

                    while (true) {
                        System.out.println("Выберите фильтр (0 для применения):");
                        int choice = Integer.parseInt(scanner.nextLine().trim());

                        if (choice == 0) {
                            break;
                        }

                        AssignmentFilter currentFilter = null;

                        switch (choice) {
                            case 1:
                                System.out.println("Введите имя пользователя:");
                                String username = scanner.nextLine().trim();
                                currentFilter = AssignmentFilters.byUsername(username);
                                break;
                            case 2:
                                System.out.println();
                                String roleName = scanner.nextLine().trim();
                                currentFilter = AssignmentFilters.byRoleName(roleName);
                                break;
                            case 3:
                                System.out.println("Выберите тип:");
                                System.out.println("1. Постоянное");
                                System.out.println("2. Временное");
                                int type = Integer.parseInt(scanner.nextLine().trim());
                                currentFilter = AssignmentFilters.byType(type == 1 ? "PERMANENT" : "TEMPORARY");
                                break;
                            case 4:
                                System.out.println("Выберите статус:");
                                System.out.println("1. Активное");
                                System.out.println("2. Неактивное");
                                int status = Integer.parseInt(scanner.nextLine().trim());
                                currentFilter = status == 1 ? AssignmentFilters.activeOnly() : AssignmentFilters.inactiveOnly();
                                break;
                            case 5:
                                System.out.println("Введите дату (yyyy-MM-dd HH:mm):");
                                String date = scanner.nextLine().trim();
                                currentFilter = AssignmentFilters.assignedAfter(date);
                                break;
                            case 6:
                                System.out.println("Введите дату (yyyy-MM-dd HH:mm):");
                                String expiryDate = scanner.nextLine().trim();
                                currentFilter = AssignmentFilters.expiringBefore(expiryDate);
                                break;
                            default:
                                System.out.println("Неверный выбор");
                        }

                        if (currentFilter != null) {
                            filter = (filter == null) ?currentFilter : filter.and(currentFilter);
                        }
                    }

                    List<RoleAssignment> assignments = sys.getAssignmentManager().findAll(filter, AssignmentSorters.byAssignmentDate());

                    if (assignments.isEmpty()) {
                        System.out.println("Назначения не найдены");
                        return;
                    }

                    System.out.println("\n=== Результаты поиска ===");
                    for (RoleAssignment assignment : assignments) {
                        System.out.println(assignment.summary());
                        System.out.println("============================================================");
                    }
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

        parser.registerCommand("help", "Справка по командам",
                (scanner, sys) -> {
                    parser.printHelp();
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
