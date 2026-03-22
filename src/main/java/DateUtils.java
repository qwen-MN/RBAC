import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String getCurrentDate() {
        return LocalDate.now().format(DATE_FORMATTER);
    }

    public static String getCurrentDateTime() {
        return java.time.LocalDateTime.now().format(DATETIME_FORMATTER);
    }

    public static boolean isBefore(String date1, String date2) {
        return date1.compareTo(date2) < 0;
    }

    public static boolean isAfter(String date1, String date2) {
        return date1.compareTo(date2) > 0;
    }

    public static String addDays(String date, int days) {
        LocalDate d = LocalDate.parse(date, DATE_FORMATTER);
        return d.plusDays(days).format(DATE_FORMATTER);
    }

    public static String formatRelativeTime(String date) {
        LocalDate targetDate = LocalDate.parse(date, DATE_FORMATTER);
        LocalDate today = LocalDate.now();
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(today, targetDate);

        if (daysBetween == 0) {
            return "today";
        } else if (daysBetween == 1) {
            return "tomorrow";
        } else if (daysBetween == -1) {
            return "yesterday";
        } else if (daysBetween > 0) {
            return "in " + daysBetween + " days";
        } else {
            return Math.abs(daysBetween) + " days ago";
        }
    }
}