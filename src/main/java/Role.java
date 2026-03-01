import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class Role {
    private static final AtomicLong ID_COUNTER = new AtomicLong(0);

    private final String id;
    private final String name;
    private final String description;
    private final Set<Permission> permissions = new HashSet<>();

    public Role(String name, String description) {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");

        if (name.isBlank() || description.isBlank()) {
            throw new IllegalArgumentException("Name and description must be non-empty");
        }

        this.id = "role_" + ID_COUNTER.incrementAndGet();
        this.name = name.trim();
        this.description = description.trim();
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public void addPermission(Permission permission) {
        Objects.requireNonNull(permission, "Permission cannot be null");
        permissions.add(permission);
    }

    public void removePermission(Permission permission) {
        permissions.remove(permission);
    }

    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    public boolean hasPermission(String permissionName, String resource) {
        if (permissionName == null || resource == null) {
            return false;
        }

        String normalizedPermissionName = permissionName.trim().toUpperCase();
        String normalizedResource = resource.trim().toLowerCase();

        return permissions.stream()
                .anyMatch(permission ->
                        permission.name().equals(normalizedPermissionName) &&
                                permission.resource().equals(normalizedResource)
                );
    }

    public Set<Permission> getPermissions() {
        return Set.copyOf(permissions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Role role)) {
            return false;
        }
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return format();
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append("Role: ").append(name).append(" [ID: ").append(id).append("]\n");
        sb.append("Description: ").append(description).append("\n");
        sb.append("Permissions(").append(permissions.size()).append("):");

        if (permissions.isEmpty()) {
            sb.append(" (none)");
        } else {
            for (Permission p : permissions) {
                sb.append("\n - ").append(p.format());
            }
        }

        return sb.toString();
    }
}