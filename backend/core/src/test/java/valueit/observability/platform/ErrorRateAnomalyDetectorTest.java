package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import valueit.observability.platform.anomaly.Anomaly;
import valueit.observability.platform.anomaly.ErrorRateAnomalyDetector;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.model.LogEntry;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ErrorRateAnomalyDetectorTest {

    private LogEntry logWith(String level) {
        LogEntry e = new LogEntry();
        e.setLevel(level);
        e.setSource("test");
        e.setMessage("msg");
        e.setTimestamp(Instant.now());
        return e;
    }

    @Test
    void detect_belowMinLogs_returnsEmpty() {
        ErrorRateAnomalyDetector detector = new ErrorRateAnomalyDetector();
        for (int i = 0; i < 9; i++) {
            assertTrue(detector.detect(logWith("ERROR")).isEmpty());
        }
    }

    @Test
    void detect_allErrors_returnsCritical() {
        ErrorRateAnomalyDetector detector = new ErrorRateAnomalyDetector();
        Optional<Anomaly> result = Optional.empty();
        for (int i = 0; i < 10; i++) {
            result = detector.detect(logWith("ERROR"));
        }
        assertTrue(result.isPresent());
        assertEquals("HIGH_ERROR_RATE", result.get().getType());
        assertEquals(Severity.CRITICAL, result.get().getSeverity()); // 100% >= 50%
    }

    @Test
    void detect_lowErrorRate_returnsEmpty() {
        ErrorRateAnomalyDetector detector = new ErrorRateAnomalyDetector();
        for (int i = 0; i < 8; i++) {
            detector.detect(logWith("INFO"));
        }
        detector.detect(logWith("ERROR"));
        Optional<Anomaly> result = detector.detect(logWith("INFO"));
        assertTrue(result.isEmpty()); // 10% < 30%
    }

    @Test
    void detect_thirtyPercentErrors_returnsHigh() {
        ErrorRateAnomalyDetector detector = new ErrorRateAnomalyDetector();
        // 4 errors out of 11 = ~36% => above 30% but below 50%
        for (int i = 0; i < 7; i++) {
            detector.detect(logWith("INFO"));
        }
        for (int i = 0; i < 3; i++) {
            detector.detect(logWith("ERROR"));
        }
        Optional<Anomaly> result = detector.detect(logWith("ERROR"));
        assertTrue(result.isPresent());
        assertEquals(Severity.HIGH, result.get().getSeverity());
    }
}
