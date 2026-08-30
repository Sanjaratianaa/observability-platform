package valueit.observability.platform.anomaly;

import valueit.observability.platform.incident.Severity;

import java.time.Instant;

public class Anomaly {
    private String type;
    private String description;
    private Severity severity;
    private Instant detectedAt;
    private String sourceLogMessage;

    public Anomaly(String type, String description, Severity severity, String sourceLogMessage) {
        this.type = type;
        this.description = description;
        this.severity = severity;
        this.sourceLogMessage = sourceLogMessage;
        this.detectedAt = Instant.now();
    }

    public String getType() { return type; }
    public String getDescription() { return description; }
    public Severity getSeverity() { return severity; }
    public Instant getDetectedAt() { return detectedAt; }
    public String getSourceLogMessage() { return sourceLogMessage; }
}