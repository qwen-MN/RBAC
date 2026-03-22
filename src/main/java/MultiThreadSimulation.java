import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.text.DecimalFormat;

public class MultiThreadSimulation {
    private static final int PROGRESS_WIDTH = 50;
    private static final char PROGRESS_CHAR = '+';
    private static final char EMPTY_CHAR = '-';
    private static final DecimalFormat DF = new DecimalFormat("0.0");

    private final int threadCount;
    private final int calculationLength;
    private final ExecutorService executor;
    private final AtomicInteger completedThreads;
    private final ThreadLocal<Integer> lineIndexHolder = new ThreadLocal<>();
    private final Object[] lineLocks = new Object[100];

    public MultiThreadSimulation(int threadCount, int calculationLength) {
        this.threadCount = threadCount;
        this.calculationLength = calculationLength;
        this.executor = Executors.newFixedThreadPool(threadCount);
        this.completedThreads = new AtomicInteger(0);

        for (int i = 0; i < lineLocks.length; i++) {
            lineLocks[i] = new Object();
        }
    }

    public void start() {
        System.out.println("Запуск " + threadCount + " потоков (длина: " + calculationLength + ")\n");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int lineIndex = i;
            executor.submit(() -> {
                lineIndexHolder.set(lineIndex);
                simulateCalculation(lineIndex);
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long totalTime = System.currentTimeMillis() - startTime;
        System.out.println("\nВсе потоки завершены. Общее время: " + totalTime + " мс");
    }

    private void simulateCalculation(int lineIndex) {
        long threadStart = System.currentTimeMillis();
        long threadId = Thread.currentThread().threadId();
        int threadNumber = lineIndex + 1;

        int delayMs = Math.max(30, calculationLength / 80);

        moveToLine(lineIndex);
        printInitialLine(threadNumber, threadId);

        for (int i = 1; i <= PROGRESS_WIDTH; i++) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                return;
            }

            long elapsed = System.currentTimeMillis() - threadStart;
            updateOwnLine(lineIndex, threadNumber, threadId, i, elapsed);
        }

        long threadTime = System.currentTimeMillis() - threadStart;
        moveToLine(lineIndex);
        printFinalLine(threadNumber, threadId, threadTime);

        completedThreads.incrementAndGet();
    }

    private void moveToLine(int lineIndex) {
        System.out.printf("\033[%d;1H", lineIndex + 3);
    }

    private void printInitialLine(int threadNumber, long threadId) {
        System.out.print("\033[2K");
        System.out.printf("Поток %2d | TID:%-12d | [%50s] %5s%% | %8s%n",
                threadNumber, threadId,
                String.valueOf(EMPTY_CHAR).repeat(PROGRESS_WIDTH),
                "0.0", "0 мс");
        System.out.flush();
    }

    private void updateOwnLine(int lineIndex, int threadNumber, long threadId,
                               int progress, long elapsed) {
        moveToLine(lineIndex);
        System.out.print("\033[2K");

        double percent = (double) progress / PROGRESS_WIDTH * 100;
        String bar = String.valueOf(PROGRESS_CHAR).repeat(progress) +
                String.valueOf(EMPTY_CHAR).repeat(PROGRESS_WIDTH - progress);

        System.out.printf("Поток %2d | TID:%-12d | [%50s] %5s%% | %8s%n",
                threadNumber, threadId, bar, DF.format(percent), elapsed + " мс");
        System.out.flush();
    }

    private void printFinalLine(int threadNumber, long threadId, long threadTime) {
        moveToLine(lineIndexHolder.get());
        System.out.printf("Поток %2d | TID:%-12d | [%50s] %5s%% | %8s\033[0m%n",
                threadNumber, threadId,
                String.valueOf(PROGRESS_CHAR).repeat(PROGRESS_WIDTH),
                "100.0", threadTime + " мс");
        System.out.flush();
    }

    public static void main(String[] args) {
        int threadCount = 6;
        int calculationLength = 5000;

        if (args.length >= 2) {
            threadCount = Integer.parseInt(args[0]);
            calculationLength = Integer.parseInt(args[1]);
        }

        new MultiThreadSimulation(threadCount, calculationLength).start();
    }
}
