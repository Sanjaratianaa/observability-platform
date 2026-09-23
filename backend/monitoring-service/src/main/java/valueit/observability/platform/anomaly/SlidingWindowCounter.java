package valueit.observability.platform.anomaly;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Compteur à fenêtre glissante : conserve les niveaux de logs récents
 * et calcule le taux d'erreurs sur la fenêtre configurée.
 */
public class SlidingWindowCounter {

    private record Entry(Instant timestamp, String level) {}

    private final Deque<Entry> entries = new ConcurrentLinkedDeque<>();
    private final long windowSeconds;

    public SlidingWindowCounter(long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public void record(String level) {
        entries.addLast(new Entry(Instant.now(), level));
        evict();
    }

    public double errorRate() {
        evict();
        if (entries.isEmpty()) {
            return 0.0;
        }
        long errors = entries.stream()
                .filter(e -> "ERROR".equalsIgnoreCase(e.level()))
                .count();
        return (double) errors / entries.size();
    }

    public long totalCount() {
        evict();
        return entries.size();
    }

    private void evict() {
        Instant cutoff = Instant.now().minusSeconds(windowSeconds);
        while (!entries.isEmpty() && entries.peekFirst().timestamp().isBefore(cutoff)) {
            entries.pollFirst();
        }
    }
}
