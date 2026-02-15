import java.util.Objects;
import java.util.regex.Pattern;

public record Permission(String name, String resource, String description) {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Z0-9_]+$");
    private static final Pattern RESOURCE_PATTERN = Pattern.compile("^[a-z0-9_]+$");

    public Permission {
        Objects.requireNonNull(name, "Name cannot be null");
        Objects.requireNonNull(resource, "Resource cannot be null");
        Objects.requireNonNull(description, "Description cannot be null");

        name = name.trim().toUpperCase();
        resource = resource.trim().toLowerCase();
        description = description.trim();

        if (name.isEmpty() || resource.isEmpty() || description.isEmpty()) {
            throw new IllegalArgumentException("All fields must be non-empty after normalization");
        }

        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new IllegalArgumentException("Permission name must be uppercase without spaces");
        }

        if (!RESOURCE_PATTERN.matcher(resource).matches()) {
            throw new IllegalArgumentException("Resource must be lowercase alphanumeric");
        }
    }

    public String format() {
        return String.format("%s on %s: %s", name(), resource(), description());
    }

    public boolean matches(String namePattern, String resourcePattern) {
        boolean nameMatch = namePattern == null || namePattern.isEmpty() ||
                name().contains(namePattern.toUpperCase());
        boolean resourceMatch = resourcePattern == null || resourcePattern.isEmpty() ||
                resource().contains(resourcePattern.toLowerCase());
        return nameMatch && resourceMatch;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)  {
            return true;
        }
        if (!(o instanceof Permission that)) {
            return false;
        }
        return Objects.equals(name(), that.name()) &&
                Objects.equals(resource(), that.resource());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name(), resource());
    }
}