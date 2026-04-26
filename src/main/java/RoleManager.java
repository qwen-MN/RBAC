import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.function.Predicate;

public class RoleManager {
    private final Map<String, Role> rolesByName = new ConcurrentHashMap<>();
    private final Map<String, Role> rolesById = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void add(Role role) {
        lock.writeLock().lock();
        try {
            if (role == null) {
                throw new IllegalArgumentException("Role cannot be null");
            }

            if (role.name() == null || role.name().trim().isEmpty()) {
                throw new IllegalArgumentException("Role name cannot be null or empty");
            }

            if (rolesByName.containsKey(role.name())) {
                throw new IllegalArgumentException("Role with name '" + role.name() + "' already exists");
            }

            rolesByName.put(role.name(), role);
            rolesById.put(role.id(), role);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean remove(Role role) {
        lock.writeLock().lock();
        try {
            boolean roleExists = rolesByName.containsKey(role.name());

            if (roleExists) {
                rolesByName.remove(role.name());
                rolesById.remove(role.id());
            }

            return roleExists;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean exists(String roleName) {
        return rolesByName.containsKey(roleName);
    }

    public Optional<Role> findByName(String name) {
        return Optional.ofNullable(rolesByName.get(name));
    }

    public Optional<Role> findById(String id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(rolesById.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findAll(Predicate<Role> filter, Comparator<Role> sorter) {
        lock.readLock().lock();
        try {
            return rolesByName.values().stream()
                    .filter(filter != null ? filter : r -> true)
                    .sorted(sorter != null ? sorter : (a, b) -> 0)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Role> findByFilterParallel(Predicate<Role> filter, Comparator<Role> sorter) {
        lock.readLock().lock();
        try {
            return rolesByName.values().parallelStream()
                    .filter(filter != null ? filter : r -> true)
                    .sorted(sorter != null ? sorter : (a, b) -> 0)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addPermissionToRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try {
            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new NoSuchElementException("Role not found: " + roleName);
            }

            role.addPermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void removePermissionFromRole(String roleName, Permission permission) {
        lock.writeLock().lock();
        try {
            Role role = rolesByName.get(roleName);
            if (role == null) {
                throw new NoSuchElementException("Role not found: " + roleName);
            }

            role.removePermission(permission);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public List<Role> findRolesWithPermission(String permissionName, String resource) {
        lock.readLock().lock();
        try {
            return rolesByName.values().stream()
                    .filter(role -> role.hasPermission(permissionName, resource))
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public int count() {
        return rolesByName.size();
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            rolesByName.clear();
            rolesById.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}
