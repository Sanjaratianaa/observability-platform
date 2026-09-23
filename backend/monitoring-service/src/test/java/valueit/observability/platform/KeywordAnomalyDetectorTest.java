package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import valueit.observability.platform.anomaly.Anomaly;
import valueit.observability.platform.anomaly.KeywordAnomalyDetector;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.model.LogEntry;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KeywordAnomalyDetectorTest {

    private final KeywordAnomalyDetector detector = new KeywordAnomalyDetector();

    private LogEntry logWith(String message) {
        LogEntry e = new LogEntry();
        e.setLevel("ERROR");
        e.setSource("test");
        e.setMessage(message);
        e.setTimestamp(Instant.now());
        return e;
    }

    @Test
    void detect_oom_returnsCritical() {
        Optional<Anomaly> result = detector.detect(logWith("java.lang.OutOfMemoryError: Java heap space"));
        assertTrue(result.isPresent());
        assertEquals(Severity.CRITICAL, result.get().getSeverity());
        assertEquals("KEYWORD_OUTOFMEMORYERROR", result.get().getType());
    }

    @Test
    void detect_deadlock_returnsCritical() {
        Optional<Anomaly> result = detector.detect(logWith("Thread deadlock detected in pool-3"));
        assertTrue(result.isPresent());
        assertEquals(Severity.CRITICAL, result.get().getSeverity());
    }

    @Test
    void detect_connectionRefused_returnsHigh() {
        Optional<Anomaly> result = detector.detect(logWith("Connection refused to database server"));
        assertTrue(result.isPresent());
        assertEquals(Severity.HIGH, result.get().getSeverity());
    }

    @Test
    void detect_timeout_returnsMedium() {
        Optional<Anomaly> result = detector.detect(logWith("Request timeout after 30s"));
        assertTrue(result.isPresent());
        assertEquals(Severity.MEDIUM, result.get().getSeverity());
    }

    @Test
    void detect_normalMessage_returnsEmpty() {
        assertTrue(detector.detect(logWith("Application started successfully")).isEmpty());
    }

    @Test
    void detect_nullMessage_returnsEmpty() {
        LogEntry e = new LogEntry();
        e.setLevel("INFO");
        assertTrue(detector.detect(e).isEmpty());
    }

    @Test
    void detect_longMessage_truncatesInDescription() {
        String longMsg = "OutOfMemoryError " + "x".repeat(200);
        Optional<Anomaly> result = detector.detect(logWith(longMsg));
        assertTrue(result.isPresent());
        assertTrue(result.get().getDescription().contains("..."));
    }
}
