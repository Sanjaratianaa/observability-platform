package valueit.observability.platform.anomaly;

import org.springframework.stereotype.Component;
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
            "Caused by:\\s([\\w.]+Exception)"
    );

    @Override
    public Optional<Anomaly> detect(LogEntry entry) {
        if (entry.getMessage() == null || !entry.getMessage().contains("Exception")) {
            return Optional.empty();
        }

        String message = entry.getMessage();

        Matcher rootCauseMatcher = ROOT_CAUSE_PATTERN.matcher(message);
        String rootCause = rootCauseMatcher.find() ? rootCauseMatcher.group(1) : extractFirstExceptionType(message);

        Matcher frameMatcher = STACK_FRAME_PATTERN.matcher(message);
        String location = frameMatcher.find()
                ? frameMatcher.group(1) + "." + frameMatcher.group(2) + " (ligne " + frameMatcher.group(4) + ")"
                : "localisation inconnue";

        String severity = classifySeverity(rootCause);

        Anomaly anomaly = new Anomaly(
                "STACK_TRACE_EXCEPTION",
                rootCause + " détectée dans " + location,
                severity,
                message
        );

        return Optional.of(anomaly);
    }

    private String extractFirstExceptionType(String message) {
        Pattern pattern = Pattern.compile("([\\w.]+Exception)");
        Matcher matcher = pattern.matcher(message);
        return matcher.find() ? matcher.group(1) : "Exception inconnue";
    }

    private String classifySeverity(String exceptionType) {
        if (exceptionType.contains("NullPointerException") ||
            exceptionType.contains("OutOfMemoryError")) {
            return "HIGH";
        }

        if (exceptionType.contains("Exception")) {
            return "MEDIUM";
        }

        return "LOW";
    }
}