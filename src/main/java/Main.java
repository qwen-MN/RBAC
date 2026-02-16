import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== RBAC System Tests ===\n");

        testUserValidation();
        System.out.println();

        testPermissionValidation();
        System.out.println();

        testRoleManagement();
        System.out.println();

        testAssignments();
        System.out.println();

        testTemporaryAssignmentExpiry();
        System.out.println();

        System.out.println("All tests completed successfully!");
    }

    private static void testUserValidation() {
        System.out.println("1. User Validation Tests:");

        try {
            User.create("john_doe", "John Doe", "john@example.com");
            System.out.println("Valid user created");
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage());
        }

        try {
            User.create("ab", "Short", "test@example.com");
            System.out.println("Should fail: username too short");
        } catch (IllegalArgumentException e) {
            System.out.println("Correctly rejected short username");
        }

        try {
            User.create("invalid username", "Test", "test@example.com");
            System.out.println("Should fail: space in username");
        } catch (IllegalArgumentException e) {
            System.out.println("Correctly rejected username with space");
        }

        try {
            User.create("valid_user", "Test", "invalid-email");
            System.out.println("Should fail: invalid email");
        } catch (IllegalArgumentException e) {
            System.out.println("Correctly rejected invalid email");
        }
    }

    private static void testPermissionValidation() {
        System.out.println("2. Permission Validation Tests:");

        Permission p1 = new Permission("READ", "users", "Can view users");
        System.out.println("Created permission: " + p1.format());

        Permission p2 = new Permission("write", "REPORTS", "Can edit reports");
        System.out.println("Normalized permission: " + p2.format());

        System.out.println("Search match: " + p1.matches("REA", "user"));
        System.out.println("Search no match: " + p1.matches("DELETE", null));
    }

    private static void testRoleManagement() {
        System.out.println("3. Role Management Tests:");

        Role admin = new Role("Administrator", "Full system access");
        admin.addPermission(new Permission("READ", "users", "View users"));
        admin.addPermission(new Permission("WRITE", "users", "Edit users"));
        admin.addPermission(new Permission("DELETE", "users", "Delete users"));

        System.out.println(admin.format());

        System.out.println("Has READ permission: " +
                admin.hasPermission("READ", "users"));
        System.out.println("Has DELETE permission: " +
                admin.hasPermission("DELETE", "users"));
        System.out.println("Missing EXECUTE permission: " +
                !admin.hasPermission("EXECUTE", "users"));
    }

    private static void testAssignments() {
        System.out.println("4. Assignment Tests:");

        User user = User.create("jane_smith", "Jane Smith", "jane@example.com");
        Role viewer = new Role("Viewer", "Read-only access");
        viewer.addPermission(new Permission("READ", "reports", "View reports"));

        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Initial setup");
        PermanentAssignment permAssign = new PermanentAssignment(user, viewer, meta);

        System.out.println(permAssign.summary());
        System.out.println("Assignment active: " + permAssign.isActive());

        permAssign.revoke();
        System.out.println("After revoke, active: " + permAssign.isActive());
    }

    private static void testTemporaryAssignmentExpiry() {
        System.out.println("5. Temporary Assignment Tests:");

        User user = User.create("bob_jones", "Bob Jones", "bob@example.com");
        Role editor = new Role("Editor", "Can edit content");
        editor.addPermission(new Permission("WRITE", "articles", "Edit articles"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String futureDate = LocalDateTime.now().plusHours(1).format(formatter);

        AssignmentMetadata meta = AssignmentMetadata.now("admin", "Temporary access");
        TemporaryAssignment tempAssign = new TemporaryAssignment(
                user, editor, meta, futureDate, false
        );

        System.out.println(tempAssign.summary());
        System.out.println("Time remaining: " + tempAssign.getTimeRemaining());
        System.out.println("Is active: " + tempAssign.isActive());

        String extendedDate = LocalDateTime.now().plusDays(7).format(formatter);
        tempAssign.extend(extendedDate);
        System.out.println("After extension, time remaining: " + tempAssign.getTimeRemaining());
    }
}