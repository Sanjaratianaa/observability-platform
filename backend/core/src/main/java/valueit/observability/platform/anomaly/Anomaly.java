package valueit.observability.platform.anomaly;

import java.time.Instant;

public class Anomaly {
    private String type;
    private String description;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private Instant detectedAt;
    private String sourceLogMessage;

    public Anomaly(String type, String description, String severity, String sourceLogMessage) {
        this.type = type;
        this.description = description;
        this.severity = severity;
        this.sourceLogMessage = sourceLogMessage;
        this.detectedAt = Instant.now();
    }

    public String getType() { return type; }
    public String getDescription() { return description; }
    public String getSeverity() { return severity; }
    public Instant getDetectedAt() { return detectedAt; }
    public String getSourceLogMessage() { return sourceLogMessage; }
}