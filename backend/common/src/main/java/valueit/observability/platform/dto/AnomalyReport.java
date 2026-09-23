package valueit.observability.platform.dto;

import valueit.observability.platform.incident.Severity;

import java.time.Instant;

/**
 * Contrat inter-services : envoyé par monitoring-service à incident-service
 * via POST /internal/anomalies lorsqu'une anomalie est détectée sur un log.
 */
public record AnomalyReport(
        String type,
        String description,
        Severity severity,
        Instant detectedAt,
        String source,
        String logId,
        String sourceLogMessage
) {}
