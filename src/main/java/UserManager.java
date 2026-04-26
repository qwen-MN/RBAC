import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.function.Predicate;

public class UserManager {
    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public void add(User user) {
        synchronized (this) {
            if (usersByUsername.containsKey(user.username())) {
                throw new IllegalArgumentException("User already exists: " + user.username());
            }
            usersByUsername.put(user.username(), user);
            usersByEmail.put(user.email(), user);
        }
    }

    public void update(String username, String fullName, String email) {
        lock.writeLock().lock();
        try {
            User user = usersByUsername.get(username);
            if (user == null) {
                throw new NoSuchElementException("User not found: " + username);
            }

            if (!user.email().equals(email) && usersByEmail.containsKey(email)) {
                throw new IllegalArgumentException("Email already exists: " + email);
            }

            if (!ValidationUtils.isValidEmail(email)) {
                throw new IllegalArgumentException("Invalid email format: " + email);
            }

            User updatedUser = User.create(username, fullName, email);

            usersByEmail.remove(user.email());
            usersByUsername.put(username, updatedUser);
            usersByEmail.put(email, updatedUser);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean remove(User user) {
        lock.writeLock().lock();
        try {
            boolean userExists = usersByUsername.containsKey(user.username());
            if (userExists) {
                usersByUsername.remove(user.username());
                usersByEmail.remove(user.email());
            }
            return userExists;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean exists(String username) {
        synchronized (this) {
            return usersByUsername.containsKey(username);
        }
    }

    public Optional<User> findByUsername(String username) {
        synchronized (this) {
            return Optional.ofNullable(usersByUsername.get(username));
        }
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    public List<User> findAll(Predicate<User> filter, Comparator<User> sorter) {
        lock.readLock().lock();
        try {
            return usersByUsername.values().stream()
                    .filter(filter != null ? filter : u -> true)
                    .sorted(sorter != null ? sorter : (a, b) -> 0)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<User> findByFilterParallel(Predicate<User> filter, Comparator<User> sorter) {
        lock.readLock().lock();
        try {
            return usersByUsername.values().stream()
                    .filter(filter != null ? filter : u -> true)
                    .sorted(sorter != null ? sorter : (a, b) -> 0)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    public int count() {
        return usersByUsername.size();
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            usersByUsername.clear();
            usersByEmail.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        UserManager that = (UserManager) obj;
        return Objects.equals(usersByUsername, that.usersByUsername);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usersByUsername);
    }
}