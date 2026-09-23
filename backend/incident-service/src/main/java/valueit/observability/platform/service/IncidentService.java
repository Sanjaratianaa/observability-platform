package valueit.observability.platform.service;

import org.springframework.stereotype.Service;
import valueit.observability.platform.dto.AnomalyReport;
import valueit.observability.platform.metadata.AuditService;
import valueit.observability.platform.metrics.PlatformMetrics;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentStatus;
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

    public Incident handle(AnomalyReport report) {
        String fingerprint = buildFingerprint(report);

        Optional<Incident> existing =
                incidentRepository.findFirstByFingerprintAndStatusIn(fingerprint, ACTIVE_STATUSES);

        if (existing.isPresent()) {
            return updateRecurring(existing.get(), report);
        }
        return createNew(fingerprint, report);
    }

    private Incident updateRecurring(Incident incident, AnomalyReport report) {
        incident.setLastSeen(Instant.now());
        incident.setOccurrenceCount(incident.getOccurrenceCount() + 1);
        incident.setSeverity(incident.getSeverity().max(report.severity()));
        incident.setDescription(report.description());
        if (report.logId() != null) {
            incident.addRelatedLogId(report.logId());
        }
        Incident saved = incidentRepository.save(incident);
        notificationHub.dispatch(saved, IncidentEvent.RECURRED);
        return saved;
    }

    private Incident createNew(String fingerprint, AnomalyReport report) {
        Instant now = Instant.now();
        Incident incident = new Incident();
        incident.setFingerprint(fingerprint);
        incident.setType(report.type());
        incident.setSeverity(report.severity());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setSource(report.source());
        incident.setDescription(report.description());
        incident.setFirstSeen(now);
        incident.setLastSeen(now);
        incident.setOccurrenceCount(1);
        if (report.logId() != null) {
            incident.addRelatedLogId(report.logId());
        }

        Incident saved = incidentRepository.save(incident);
        metrics.incidentCreated();
        notificationHub.dispatch(saved, IncidentEvent.CREATED);
        auditService.record("INCIDENT_CREATED", "INCIDENT", saved.getId(), report.description(), "system");
        return saved;
    }

    private String buildFingerprint(AnomalyReport report) {
        return report.type() + "::" + report.source();
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
