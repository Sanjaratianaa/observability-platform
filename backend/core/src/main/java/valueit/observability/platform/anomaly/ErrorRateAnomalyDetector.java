package valueit.observability.platform.anomaly;

import org.springframework.stereotype.Component;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.model.LogEntry;

import java.util.Optional;

@Component
public class ErrorRateAnomalyDetector implements AnomalyDetector {

    private static final long WINDOW_SECONDS = 300; // 5 minutes
    private static final double ERROR_RATE_THRESHOLD = 0.30; // 30%
    private static final int MIN_LOGS_BEFORE_CHECKING = 10; // évite les faux positifs sur peu de données

    private final SlidingWindowCounter counter = new SlidingWindowCounter(WINDOW_SECONDS);

    @Override
    public Optional<Anomaly> detect(LogEntry entry) {
        counter.record(entry.getLevel());

        if (counter.totalCount() < MIN_LOGS_BEFORE_CHECKING) {
            return Optional.empty();
        }

        double rate = counter.errorRate();

        if (rate >= ERROR_RATE_THRESHOLD) {
            Anomaly anomaly = new Anomaly(
                    "HIGH_ERROR_RATE",
                    String.format("Taux d'erreur élevé : %.0f%% sur les %d dernières minutes (%d logs analysés)",
                            rate * 100, WINDOW_SECONDS / 60, counter.totalCount()),
                    rate >= 0.5 ? Severity.CRITICAL : Severity.HIGH,
                    entry.getMessage()
            );
            return Optional.of(anomaly);
        }

        return Optional.empty();
    }
}