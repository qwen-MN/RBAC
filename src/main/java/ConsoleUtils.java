import java.util.List;
import java.util.Scanner;

public class ConsoleUtils {

    public static String promptString(Scanner scanner, String message, boolean required) {
        while (true) {
            System.out.print(message + (required ? " (required): " : " (optional): "));
            String input = scanner.nextLine().trim();

            if (!required && input.isEmpty()) {
                return "";
            }

            if (input.isEmpty()) {
                System.out.println("Input cannot be empty. Please try again.");
                continue;
            }

            return input;
        }
    }

    public static int promptInt(Scanner scanner, String message, int min, int max) {
        while (true) {
            System.out.print(message + " [" + min + "-" + max + "]: ");

            try {
                int value = Integer.parseInt(scanner.nextLine().trim());

                if (value < min || value > max) {
                    System.out.println("Value must be between " + min + " and " + max);
                    continue;
                }

                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }

    public static boolean promptYesNo(Scanner scanner, String message) {
        while (true) {
            System.out.print(message + " (yes/no): ");
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("yes") || input.equals("y") ||
                    input.equals("да") || input.equals("д")) {
                return true;
            }

            if (input.equals("no") || input.equals("n") ||
                    input.equals("нет") || input.equals("н")) {
                return false;
            }

            System.out.println("Please enter 'yes' or 'no'");
        }
    }

    public static <T> T promptChoice(Scanner scanner, String message, List<T> options) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("Options list cannot be empty");
        }

        if (options.size() == 1) {
            return options.get(0);
        }

        System.out.println(message);

        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + options.get(i));
        }

        while (true) {
            System.out.print("Enter your choice [1-" + options.size() + "]: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine().trim());

                if (choice < 1 || choice > options.size()) {
                    System.out.println("Invalid choice. Please try again.");
                    continue;
                }

                return options.get(choice - 1);
            } catch (NumberFormatException e) {
                System.out.println("Invalid number. Please try again.");
            }
        }
    }
}
