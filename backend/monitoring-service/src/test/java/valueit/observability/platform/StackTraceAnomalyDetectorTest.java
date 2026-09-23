package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import valueit.observability.platform.anomaly.Anomaly;
import valueit.observability.platform.anomaly.StackTraceAnomalyDetector;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.model.LogEntry;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StackTraceAnomalyDetectorTest {

    private final StackTraceAnomalyDetector detector = new StackTraceAnomalyDetector();

    private LogEntry logWith(String message) {
        LogEntry e = new LogEntry();
        e.setLevel("ERROR");
        e.setSource("test");
        e.setMessage(message);
        e.setTimestamp(Instant.now());
        return e;
    }

    @Test
    void detect_noException_returnsEmpty() {
        assertTrue(detector.detect(logWith("Everything is fine")).isEmpty());
    }

    @Test
    void detect_nullMessage_returnsEmpty() {
        LogEntry e = new LogEntry();
        e.setLevel("ERROR");
        assertTrue(detector.detect(e).isEmpty());
    }

    @Test
    void detect_npe_returnsHighSeverity() {
        String msg = "java.lang.NullPointerException at com.example.Service.process(Service.java:42)";
        Optional<Anomaly> result = detector.detect(logWith(msg));

        assertTrue(result.isPresent());
        assertEquals("STACK_TRACE_EXCEPTION", result.get().getType());
        assertEquals(Severity.HIGH, result.get().getSeverity());
        assertTrue(result.get().getDescription().contains("NullPointerException"));
        assertTrue(result.get().getDescription().contains("ligne 42"));
    }

    @Test
    void detect_causedBy_extractsRootCause() {
        String msg = "Error processing\nCaused by: com.example.CustomException\n  at com.example.Dao.query(Dao.java:10)";
        Optional<Anomaly> result = detector.detect(logWith(msg));

        assertTrue(result.isPresent());
        assertTrue(result.get().getDescription().contains("CustomException"));
        assertEquals(Severity.MEDIUM, result.get().getSeverity());
    }

    @Test
    void detect_genericException_returnsMediumSeverity() {
        String msg = "java.io.IOException at com.example.IO.read(IO.java:5)";
        Optional<Anomaly> result = detector.detect(logWith(msg));

        assertTrue(result.isPresent());
        assertEquals(Severity.MEDIUM, result.get().getSeverity());
    }

    @Test
    void detect_looseExceptionWording_returnsHigh() {
        // Cas réel du sample json.log : "NullPointer exception" (minuscule, mot séparé)
        Optional<Anomaly> result = detector.detect(logWith("NullPointer exception"));

        assertTrue(result.isPresent());
        assertEquals(Severity.HIGH, result.get().getSeverity());
        assertTrue(result.get().getDescription().contains("NullPointer"));
    }

    @Test
    void detect_errorType_returnsHigh() {
        Optional<Anomaly> result = detector.detect(logWith("java.lang.OutOfMemoryError: Java heap space"));

        assertTrue(result.isPresent());
        assertEquals(Severity.HIGH, result.get().getSeverity());
        assertTrue(result.get().getDescription().contains("OutOfMemoryError"));
    }

    @Test
    void detect_benignErrorWord_returnsEmpty() {
        // "error" seul n'est pas une stack trace
        assertTrue(detector.detect(logWith("handled error")).isEmpty());
    }
}
