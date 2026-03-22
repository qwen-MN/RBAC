import java.util.List;

public class FormatUtils {

    public static String formatTable(String[] headers, List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return "";
        }

        int[] columnWidths = new int[headers.length];

        for (int i = 0; i < headers.length; i++) {
            columnWidths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < Math.min(row.length, headers.length); i++) {
                if (row[i].length() > columnWidths[i]) {
                    columnWidths[i] = row[i].length();
                }
            }
        }

        StringBuilder table = new StringBuilder();

        table.append(buildHorizontalLine(columnWidths)).append("\n");

        table.append("|");
        for (int i = 0; i < headers.length; i++) {
            table.append(" ").append(padRight(headers[i], columnWidths[i])).append(" |");
        }
        table.append("\n");

        table.append(buildHorizontalLine(columnWidths)).append("\n");

        for (String[] row : rows) {
            table.append("|");
            for (int i = 0; i < headers.length; i++) {
                String cell = (i < row.length) ? row[i] : "";
                table.append(" ").append(padRight(cell, columnWidths[i])).append(" |");
            }
            table.append("\n");
        }

        table.append(buildHorizontalLine(columnWidths));

        return table.toString();
    }

    private static String buildHorizontalLine(int[] columnWidths) {
        StringBuilder line = new StringBuilder("+");
        for (int width : columnWidths) {
            line.append("-".repeat(width + 2)).append("+");
        }
        return line.toString();
    }

    public static String formatBox(String text) {
        String[] lines = text.split("\n");
        int maxWidth = 0;
        for (String line : lines) {
            if (line.length() > maxWidth) {
                maxWidth = line.length();
            }
        }

        StringBuilder box = new StringBuilder();
        box.append("|").append("-".repeat(maxWidth + 2)).append("|\n");

        for (String line : lines) {
            box.append("| ").append(padRight(line, maxWidth)).append(" |\n");
        }

        box.append("|").append("-".repeat(maxWidth + 2)).append("|");

        return box.toString();
    }

    public static String formatHeader(String text) {
        String separator = "=".repeat(60);
        return "\n" + separator + "\n" + text + "\n" + separator;
    }

    public static String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    public static String padRight(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        return text + " ".repeat(length - text.length());
    }

    public static String padLeft(String text, int length) {
        if (text == null) {
            text = "";
        }
        if (text.length() >= length) {
            return text;
        }
        return " ".repeat(length - text.length()) + text;
    }
}