package valueit.observability.platform.anomaly;

import valueit.observability.platform.model.LogEntry;
import java.util.List;
import java.util.Optional;

public interface AnomalyDetector {
    Optional<Anomaly> detect(LogEntry entry);
    default String getName() {
        return getClass().getSimpleName();
    }
}