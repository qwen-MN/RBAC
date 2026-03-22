import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UserManager implements Repository<User>{

    private final Map<String, User> users = new HashMap<>();

    @Override
    public void add(User user) {
        if (exists(user.username())) {
            throw new IllegalArgumentException("User with username '" + user.username() + "' already exists");
        }
        users.put(user.username(), user);
    }

    @Override
    public boolean remove(User user) {
        return users.remove(user.username()) != null;
    }

    @Override
    public Optional<User> findById(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public int count() {
        return users.size();
    }

    @Override
    public void clear() {
        users.clear();
    }

    public Optional<User> findByUsername(String username) {
        return findById(username);
    }

    public Optional<User> findByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.email().equals(email))
                .findFirst();
    }

    public List<User> findByFilter(UserFilter filter) {
        return findAll(filter, null);
    }

    public List<User> findAll(UserFilter filter, Comparator<User> sorter) {
        Stream<User> stream = users.values().stream();

        if (filter != null) {
            stream = stream.filter(filter::test);
        }

        if (sorter != null) {
            stream = stream.sorted(sorter);
        }

        return stream.collect(Collectors.toList());
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }

    public void update(String username, String newFullName, String newEmail) {
        User existing = findById(username)
                .orElseThrow(() -> new NoSuchElementException(
                        "User '" + username + "' not found"));

        try {
            User updated = User.create(username, newFullName, newEmail);
            users.put(username, updated);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid update data: " + e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserManager that)) return false;
        return Objects.equals(users, that.users);
    }

    @Override
    public int hashCode() {
        return Objects.hash(users);
    }
}