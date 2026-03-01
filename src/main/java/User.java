import java.util.Objects;
import java.util.regex.Pattern;

public record User(String username, String fullName, String email) {
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");

    public User {
        Objects.requireNonNull(username, "Username cannot be null");
        Objects.requireNonNull(fullName, "Full name cannot be null");
        Objects.requireNonNull(email, "Email cannot be null");

        if (username.isBlank() || fullName.isBlank() || email.isBlank()) {
            throw new IllegalArgumentException("All fields must be non-empty");
        }

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Username must be 3-20 characters long and contain only Latin letters, digits and underscores"
            );
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Email must be in valid format (e.g., user@example.com)");
        }
    }

    public static User create(String username, String fullName, String email) {
        return new User(username, fullName, email);
    }

    public String format() {
        return String.format("%s (%s) <%s>", username(), fullName(), email());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof User user)) {
            return false;
        } else {
            return Objects.equals(username(), user.username());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(username());
    }
}