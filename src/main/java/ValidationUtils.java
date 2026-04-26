import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}( \\d{2}:\\d{2})?$");

    public static boolean isValidUsername(String username) {
        if (username == null || username.length() < 3 || username.length() > 20) {
            return false;
        }
        return USERNAME_PATTERN.matcher(username).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidDate(String date) {
        if (date == null || date.isEmpty()) {
            return false;
        }
        return DATE_PATTERN.matcher(date).matches();
    }

    public static String normalizeString(String input) {
        if (input == null) {
            return "";
        }
        return input.trim().toLowerCase();
    }

    public static void requireNonEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Field '" + fieldName + "' cannot be empty");
        }
    }
}