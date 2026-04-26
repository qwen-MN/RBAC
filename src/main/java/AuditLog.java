import java.io.BufferedWriter;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuditLog {

    public record AuditEntry(
            String timestamp,
            String action,
            String performer,
            String target,
            String details
    ) {}

    private final List<AuditEntry> entries = new ArrayList<>();
    private final BlockingQueue<AuditEntry> logQueue = new LinkedBlockingQueue<>();
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread logProcessorThread;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AuditLog() {
        // Запуск фонового потока для обработки логов
        logProcessorThread = new Thread(this::processLogQueue);
        logProcessorThread.setDaemon(true);
        logProcessorThread.start();
    }

    public void log(String action, String performer, String target, String details) {
        if (running.get()) {
            try {
                String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
                AuditEntry entry = new AuditEntry(timestamp, action, performer, target, details);
                logQueue.put(entry); // Блокирующая очередь
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processLogQueue() {
        while (running.get() || !logQueue.isEmpty()) {
            try {
                AuditEntry entry = logQueue.take(); // Блокирующее извлечение
                synchronized (entries) {
                    entries.add(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public List<AuditEntry> getAll() {
        synchronized (entries) {
            return new ArrayList<>(entries);
        }
    }

    public List<AuditEntry> getByPerformer(String performer) {
        synchronized (entries) {
            return entries.stream()
                    .filter(entry -> entry.performer().equals(performer))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }

    public List<AuditEntry> getByAction(String action) {
        synchronized (entries) {
            return entries.stream()
                    .filter(entry -> entry.action().equals(action))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        }
    }

    public void printLog() {
        System.out.println(FormatUtils.formatHeader("Audit Log"));

        List<AuditEntry> currentEntries = getAll();
        if (currentEntries.isEmpty()) {
            System.out.println("No audit entries found");
            return;
        }

        String[] headers = {"Timestamp", "Action", "Performer", "Target", "Details"};
        List<String[]> rows = currentEntries.stream()
                .map(entry -> new String[]{
                        entry.timestamp(),
                        entry.action(),
                        entry.performer(),
                        entry.target(),
                        entry.details()
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        System.out.println(FormatUtils.formatTable(headers, rows));
    }

    public void waitForProcessing() throws InterruptedException {
        while (!logQueue.isEmpty()) {
            Thread.sleep(10);
        }
    }

    public void saveToFile(String filename) throws Exception {
        List<AuditEntry> currentEntries = getAll();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("=== Audit Log ===\n");
            writer.write("Generated: " + LocalDateTime.now().format(TIMESTAMP_FORMATTER) + "\n\n");

            for (AuditEntry entry : currentEntries) {
                writer.write(String.format(
                        "[%s] %s | %s | %s | %s%n",
                        entry.timestamp(),
                        entry.action(),
                        entry.performer(),
                        entry.target(),
                        entry.details()
                ));
            }
        }
    }

    public void shutdown() {
        running.set(false);
        logProcessorThread.interrupt();
        try {
            logProcessorThread.join(5000); // Ждём максимум 5 секунд
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}