package valueit.observability.platform.service;

import org.springframework.stereotype.Service;
import valueit.observability.platform.metadata.AuditService;
import valueit.observability.platform.metrics.PlatformMetrics;
import valueit.observability.platform.anomaly.Anomaly;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentStatus;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.notification.NotificationHub;
import valueit.observability.platform.repository.IncidentRepository;
import valueit.observability.platform.incident.IncidentEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private static final List<IncidentStatus> ACTIVE_STATUSES =
            List.of(IncidentStatus.OPEN, IncidentStatus.ACKNOWLEDGED);

    private final IncidentRepository incidentRepository;
    private final NotificationHub notificationHub;
    private final AuditService auditService;
    private final PlatformMetrics metrics;

    public IncidentService(IncidentRepository incidentRepository, NotificationHub notificationHub,
                           AuditService auditService, PlatformMetrics metrics) {
        this.incidentRepository = incidentRepository;
        this.notificationHub = notificationHub;
        this.auditService = auditService;
        this.metrics = metrics;
    }

    public Incident handle(Anomaly anomaly, LogEntry sourceLog) {
        String fingerprint = buildFingerprint(anomaly, sourceLog);

        Optional<Incident> existing =
                incidentRepository.findFirstByFingerprintAndStatusIn(fingerprint, ACTIVE_STATUSES);

        if (existing.isPresent()) {
            return updateRecurring(existing.get(), anomaly, sourceLog);
        }
        return createNew(fingerprint, anomaly, sourceLog);
    }

    private Incident updateRecurring(Incident incident, Anomaly anomaly, LogEntry sourceLog) {
        incident.setLastSeen(Instant.now());
        incident.setOccurrenceCount(incident.getOccurrenceCount() + 1);
        incident.setSeverity(incident.getSeverity().max(anomaly.getSeverity()));
        incident.setDescription(anomaly.getDescription());
        if (sourceLog.getId() != null) {
            incident.addRelatedLogId(sourceLog.getId());
        }
        Incident saved = incidentRepository.save(incident);
        notificationHub.dispatch(saved, IncidentEvent.RECURRED);
        return saved;
    }

    private Incident createNew(String fingerprint, Anomaly anomaly, LogEntry sourceLog) {
        Instant now = Instant.now();
        Incident incident = new Incident();
        incident.setFingerprint(fingerprint);
        incident.setType(anomaly.getType());
        incident.setSeverity(anomaly.getSeverity());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSource(sourceLog.getSource());
        incident.setDescription(anomaly.getDescription());
        incident.setFirstSeen(now);
        incident.setLastSeen(now);
        incident.setOccurrenceCount(1);
        if (sourceLog.getId() != null) {
            incident.addRelatedLogId(sourceLog.getId());
        }

        Incident saved = incidentRepository.save(incident);
        metrics.incidentCreated();
        notificationHub.dispatch(saved, IncidentEvent.CREATED);
        auditService.record("INCIDENT_CREATED", "INCIDENT", saved.getId(), anomaly.getDescription(), "system");
        return saved;
    }

    private String buildFingerprint(Anomaly anomaly, LogEntry sourceLog) {
        return anomaly.getType() + "::" + sourceLog.getSource();
    }

    public Optional<Incident> acknowledge(String id) {
        return incidentRepository.findById(id).map(incident -> {
            if (incident.getStatus() != IncidentStatus.OPEN) {
                throw new IllegalArgumentException(
                        "Seul un incident OPEN peut être acknowledgé (statut actuel : " + incident.getStatus() + ")");
            }
            incident.setStatus(IncidentStatus.ACKNOWLEDGED);
            Incident saved = incidentRepository.save(incident);
            auditService.record("INCIDENT_ACKNOWLEDGED", "INCIDENT", id, null, "user");
            return saved;
        });
    }

    public Optional<Incident> resolve(String id) {
        return incidentRepository.findById(id).map(incident -> {
            if (incident.getStatus() == IncidentStatus.RESOLVED) {
                throw new IllegalArgumentException("Incident déjà résolu");
            }
            incident.setStatus(IncidentStatus.RESOLVED);
            Incident saved = incidentRepository.save(incident);
            metrics.incidentResolved();
            notificationHub.dispatch(saved, IncidentEvent.RESOLVED);
            auditService.record("INCIDENT_RESOLVED", "INCIDENT", id, null, "user");
            return saved;
        });
    }
}
