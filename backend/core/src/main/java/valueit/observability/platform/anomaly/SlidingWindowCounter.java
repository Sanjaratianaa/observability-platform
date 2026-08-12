package valueit.observability.platform.anomaly;

import java.time.Instant;
import java.util.concurrent.ConcurrentLinkedDeque;

public class SlidingWindowCounter {
    private final ConcurrentLinkedDeque<TimestampedLevel> recentLogs = new ConcurrentLinkedDeque<>();
    private final long windowSeconds;

    public SlidingWindowCounter(long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public void record(String level) {
        recentLogs.addLast(new TimestampedLevel(Instant.now(), level));
        evictOldEntries();
    }

    public double errorRate() {
        evictOldEntries();
        if (recentLogs.isEmpty()) return 0.0;

        long errorCount = recentLogs.stream()
                .filter(log -> "ERROR".equalsIgnoreCase(log.level()) || "CRITICAL".equalsIgnoreCase(log.level()))
                .count();

        return (double) errorCount / recentLogs.size();
    }

    public int totalCount() {
        evictOldEntries();
        return recentLogs.size();
    }

    private void evictOldEntries() {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        while (!recentLogs.isEmpty() && recentLogs.peekFirst().timestamp().isBefore(cutoff)) {
            recentLogs.pollFirst();
        }
    }

    private record TimestampedLevel(Instant timestamp, String level) {}
}