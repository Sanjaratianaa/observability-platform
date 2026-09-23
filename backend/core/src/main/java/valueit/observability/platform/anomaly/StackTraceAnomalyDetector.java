package valueit.observability.platform.anomaly;

import org.springframework.stereotype.Component;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.model.LogEntry;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StackTraceAnomalyDetector implements AnomalyDetector{
    private static final Pattern STACK_FRAME_PATTERN = Pattern.compile(
            "at\\s([\\w.$]+)\\.(\\w+)\\(([\\w.]+):(\\d+)\\)"
    );

    private static final Pattern ROOT_CAUSE_PATTERN = Pattern.compile(
            "Caused by:\\s([\\w.]+(?:Exception|Error))"
    );

    // Déclenche sur "exception" (casse libre) ou un type *Error (ex: OutOfMemoryError)
    private static final Pattern TRIGGER_PATTERN = Pattern.compile(
            "(?i:\\bexception\\b)|[\\w.$]*Error\\b"
    );

    private static final Pattern EXCEPTION_TYPE_PATTERN = Pattern.compile(
            "([\\w.$]+(?:Exception|Error))\\b"
    );

    // Cas "NullPointer exception" (mot séparé, casse libre)
    private static final Pattern LOOSE_EXCEPTION_PATTERN = Pattern.compile(
            "([\\w.$]+)\\s+(?i:exception)\\b"
    );

    @Override
    public Optional<Anomaly> detect(LogEntry entry) {
        if (entry.getMessage() == null || !TRIGGER_PATTERN.matcher(entry.getMessage()).find()) {
            return Optional.empty();
        }

        String message = entry.getMessage();

        Matcher rootCauseMatcher = ROOT_CAUSE_PATTERN.matcher(message);
        String rootCause = rootCauseMatcher.find() ? rootCauseMatcher.group(1) : extractFirstExceptionType(message);

        Matcher frameMatcher = STACK_FRAME_PATTERN.matcher(message);
        String location = frameMatcher.find()
                ? frameMatcher.group(1) + "." + frameMatcher.group(2) + " (ligne " + frameMatcher.group(4) + ")"
                : "localisation inconnue";

        Severity severity = classifySeverity(rootCause);

        Anomaly anomaly = new Anomaly(
                "STACK_TRACE_EXCEPTION",
                rootCause + " détectée dans " + location,
                severity,
                message
        );

        return Optional.of(anomaly);
    }

    private String extractFirstExceptionType(String message) {
        Matcher matcher = EXCEPTION_TYPE_PATTERN.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        Matcher loose = LOOSE_EXCEPTION_PATTERN.matcher(message);
        return loose.find() ? loose.group(1) : "Exception inconnue";
    }

    private Severity classifySeverity(String exceptionType) {
        if (exceptionType.contains("NullPointer") ||
            exceptionType.contains("OutOfMemory")) {
            return Severity.HIGH;
        }

        if (exceptionType.toLowerCase().contains("exception") ||
            exceptionType.contains("Error")) {
            return Severity.MEDIUM;
        }

        return Severity.LOW;
    }
}