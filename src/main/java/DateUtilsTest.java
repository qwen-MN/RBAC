import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DateUtils Tests")
class DateUtilsTest {

    @Test
    @DisplayName("getCurrentDate() should return YYYY-MM-DD format")
    void testGetCurrentDate() {
        String date = DateUtils.getCurrentDate();
        assertTrue(date.matches("\\d{4}-\\d{2}-\\d{2}"),
                "Date should match YYYY-MM-DD format: " + date);
    }

    @Test
    @DisplayName("getCurrentDateTime() should return YYYY-MM-DD HH:MM:SS format")
    void testGetCurrentDateTime() {
        String dateTime = DateUtils.getCurrentDateTime();
        assertTrue(dateTime.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"),
                "DateTime should match YYYY-MM-DD HH:MM:SS format: " + dateTime);
    }

    @Test
    @DisplayName("isBefore() should correctly compare dates")
    void testIsBefore() {
        assertTrue(DateUtils.isBefore("2026-01-01", "2026-12-31"));
        assertFalse(DateUtils.isBefore("2026-12-31", "2026-01-01"));
        assertFalse(DateUtils.isBefore("2026-06-15", "2026-06-15"));
    }

    @Test
    @DisplayName("isAfter() should correctly compare dates")
    void testIsAfter() {
        assertTrue(DateUtils.isAfter("2026-12-31", "2026-01-01"));
        assertFalse(DateUtils.isAfter("2026-01-01", "2026-12-31"));
        assertFalse(DateUtils.isAfter("2026-06-15", "2026-06-15"));
    }

    @Test
    @DisplayName("addDays() should add days to date")
    void testAddDays() {
        assertEquals("2026-01-02", DateUtils.addDays("2026-01-01", 1));
        assertEquals("2026-01-31", DateUtils.addDays("2026-01-01", 30));
        assertEquals("2026-02-01", DateUtils.addDays("2026-01-31", 1));
    }

    @Test
    @DisplayName("formatRelativeTime() should format relative time")
    void testFormatRelativeTime() {
        String today = DateUtils.getCurrentDate();
        String tomorrow = DateUtils.addDays(today, 1);
        String yesterday = DateUtils.addDays(today, -1);

        String relative = DateUtils.formatRelativeTime(today);
        assertTrue(relative.contains("today"), "Today should be formatted as 'today': " + relative);

        relative = DateUtils.formatRelativeTime(tomorrow);
        assertTrue(relative.contains("tomorrow"), "Tomorrow should be formatted as 'tomorrow': " + relative);

        relative = DateUtils.formatRelativeTime(yesterday);
        assertTrue(relative.contains("yesterday"), "Yesterday should be formatted as 'yesterday': " + relative);
    }
}