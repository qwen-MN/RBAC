import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoleManager implements Repository<Role> {

    private final Map<String, Role> rolesById = new HashMap<>();
    private final Map<String, Role> rolesByName = new HashMap<>();

    @Override
    public void add(Role role) {
        if (exists(role.name())) {
            throw new IllegalArgumentException(
                    "Role with name '" + role.name() + "' already exists");
        }
        rolesById.put(role.id(), role);
        rolesByName.put(role.name(), role);
    }

    @Override
    public boolean remove(Role role) {
        Role removedById = rolesById.remove(role.id());
        Role removedByName = rolesByName.remove(role.name());
        return removedById != null && removedByName != null;
    }

    @Override
    public Optional<Role> findById(String id) {
        return Optional.ofNullable(rolesById.get(id));
    }

    @Override
    public List<Role> findAll() {
        return new ArrayList<>(rolesById.values());
    }

    @Override
    public int count() {
        return rolesById.size();
    }

    @Override
    public void clear() {
        rolesById.clear();
        rolesByName.clear();
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByName.get(name));
    }

    public List<Role> findByFilter(RoleFilter filter) {
        return findAll(filter, null);
    }

    public List<Role> findAll(RoleFilter filter, Comparator<Role> sorter) {
        Stream<Role> stream = rolesById.values().stream();

        if (filter != null) {
            stream = stream.filter(filter::test);
        }

        if (sorter != null) {
            stream = stream.sorted(sorter);
        }

        return stream.collect(Collectors.toList());
    }

    public boolean exists(String name) {
        return rolesByName.containsKey(name);
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        Role role = findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException(
                        "Role '" + roleName + "' not found"));
        role.addPermission(permission);
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        Role role = findByName(roleName)
                .orElseThrow(() -> new NoSuchElementException(
                        "Role '" + roleName + "' not found"));
        role.removePermission(permission);
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        return rolesById.values().stream()
                .filter(role -> role.hasPermission(permissionName, resource))
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoleManager that)) return false;
        return Objects.equals(rolesById, that.rolesById);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rolesById);
    }
}
