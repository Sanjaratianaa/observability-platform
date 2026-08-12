package valueit.observability.platform.service;

import org.springframework.stereotype.Service;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.anomaly.AnomalyDetector;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyDetectionService {
    private final List<AnomalyDetector> detectors;

    public AnomalyDetectionService(List<AnomalyDetector> detectors) {
        this.detectors = detectors;
    }

    public List<Anomaly> analyze(LogEntry entry) {
        List<Anomaly> anomalies = new ArrayList<>();

        for (AnomalyDetector detector : detectors) {
            detector.detect(entry).ifPresent(anomalies::add);
        }

        return anomalies;
    }
}