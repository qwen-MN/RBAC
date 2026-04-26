import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FileUtils {

    private static final String FILE_HEADER = "# RBAC DATA FILE";
    private static final String FILE_VERSION = "# VERSION: 1.0";
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public static String unescape(String value) {
        if (value == null || value.isEmpty()) return value;

        return value.replace("\\r", "\r")
                .replace("\\n", "\n")
                .replace("\\|", "|")
                .replace("\\\\", "\\");
    }

    public static void saveToFile(
            String filename,
            List<String[]> users,
            List<String[]> roles,
            List<String[]> permissions,
            List<String[]> assignments
    ) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(filename), StandardCharsets.UTF_8))) {

            writer.write(FILE_HEADER);
            writer.newLine();
            writer.write(FILE_VERSION);
            writer.newLine();
            writer.write("# EXPORTED AT: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER));
            writer.newLine();
            writer.newLine();

            writer.write("[USERS]");
            writer.newLine();
            for (String[] user : users) {
                writer.write(String.join("|",
                        escape(user[0]),
                        escape(user[1]),
                        escape(user[2]),
                        escape(user[3])
                ));
                writer.newLine();
            }
            writer.newLine();

            writer.write("[ROLES]");
            writer.newLine();
            for (String[] role : roles) {
                writer.write(String.join("|",
                        escape(role[0]),
                        escape(role[1]),
                        escape(role[2]),
                        escape(role[3])
                ));
                writer.newLine();
            }
            writer.newLine();

            writer.write("[PERMISSIONS]");
            writer.newLine();
            for (String[] perm : permissions) {
                writer.write(String.join("|",
                        escape(perm[0]),
                        escape(perm[1]),
                        escape(perm[2]),
                        escape(perm[3])
                ));
                writer.newLine();
            }
            writer.newLine();

            writer.write("[ASSIGNMENTS]");
            writer.newLine();
            for (String[] assignment : assignments) {
                writer.write(String.join("|",
                        escape(assignment[0]),
                        escape(assignment[1]),
                        escape(assignment[2]),
                        escape(assignment[3]),
                        escape(assignment[4]),
                        escape(assignment[5]),
                        escape(assignment[6]),
                        escape(assignment[7])
                ));

                if (assignment.length > 8) {
                    writer.write("|" + escape(assignment[8]));
                    writer.write("|" + escape(assignment[9]));
                }

                writer.newLine();
            }
        }
    }

    public static RBACData loadFromFile(String filename) throws IOException {
        List<String[]> users = new ArrayList<>();
        List<String[]> roles = new ArrayList<>();
        List<String[]> permissions = new ArrayList<>();
        List<String[]> assignments = new ArrayList<>();

        String section = null;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filename), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("[") && line.endsWith("]")) {
                    section = line.substring(1, line.length() - 1);
                    continue;
                }

                String[] parts = line.split("\\|", -1);

                switch (section) {
                    case "USERS" -> {
                        if (parts.length >= 4) {
                            users.add(new String[]{
                                    unescape(parts[0]),
                                    unescape(parts[1]),
                                    unescape(parts[2]),
                                    unescape(parts[3])
                            });
                        }
                    }
                    case "ROLES" -> {
                        if (parts.length >= 4) {
                            roles.add(new String[]{
                                    unescape(parts[0]),
                                    unescape(parts[1]),
                                    unescape(parts[2]),
                                    unescape(parts[3])
                            });
                        }
                    }
                    case "PERMISSIONS" -> {
                        if (parts.length >= 4) {
                            permissions.add(new String[]{
                                    unescape(parts[0]),
                                    unescape(parts[1]),
                                    unescape(parts[2]),
                                    unescape(parts[3])
                            });
                        }
                    }
                    case "ASSIGNMENTS" -> {
                        if (parts.length >= 8) {
                            String[] assignment = new String[parts.length];
                            for (int i = 0; i < parts.length; i++) {
                                assignment[i] = unescape(parts[i]);
                            }
                            assignments.add(assignment);
                        }
                    }
                }
            }
        }

        return new RBACData(users, roles, permissions, assignments);
    }

    public static class RBACData {
        public final List<String[]> users;
        public final List<String[]> roles;
        public final List<String[]> permissions;
        public final List<String[]> assignments;

        public RBACData(
                List<String[]> users,
                List<String[]> roles,
                List<String[]> permissions,
                List<String[]> assignments
        ) {
            this.users = users;
            this.roles = roles;
            this.permissions = permissions;
            this.assignments = assignments;
        }
    }
}