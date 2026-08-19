package valueit.observability.platform.service;

import org.springframework.stereotype.Service;
import valueit.observability.platform.anomaly.Anomaly;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentStatus;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.notification.NotificationHub;
import valueit.observability.platform.repository.IncidentRepository;
import valueit.observability.platform.incident.IncidentEvent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final NotificationHub notificationHub;

    private static final List<String> SEVERITY_ORDER = List.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    public IncidentService(IncidentRepository incidentRepository, NotificationHub notificationHub) {
        this.incidentRepository = incidentRepository;
        this.notificationHub = notificationHub;
    }

    public Incident handle(Anomaly anomaly, LogEntry sourceLog) {
        String fingerprint = buildFingerprint(anomaly, sourceLog);

        // CORRÉLATION : existe-t-il déjà un incident OUVERT pour cette empreinte ?
        Optional<Incident> existing =
                incidentRepository.findByFingerprintAndStatus(fingerprint, IncidentStatus.OPEN);

        if (existing.isPresent()) {
            return updateRecurring(existing.get(), anomaly);   // récurrence
        }
        return createNew(fingerprint, anomaly, sourceLog);     // nouvel incident
    }

    private Incident updateRecurring(Incident incident, Anomaly anomaly) {
        incident.setLastSeen(Instant.now());
        incident.setOccurrenceCount(incident.getOccurrenceCount() + 1);
        incident.setSeverity(maxSeverity(incident.getSeverity(), anomaly.getSeverity()));
        incident.setDescription(anomaly.getDescription());
        Incident saved = incidentRepository.save(incident);
        notificationHub.dispatch(saved, IncidentEvent.RECURRED);  // ← Jira commentera, Teams se taira
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

        Incident saved = incidentRepository.save(incident);
        notificationHub.dispatch(saved, IncidentEvent.CREATED);
        return saved;
    }

    private String buildFingerprint(Anomaly anomaly, LogEntry sourceLog) {
        return anomaly.getType() + "::" + sourceLog.getSource();
    }

    private String maxSeverity(String a, String b) {
        return SEVERITY_ORDER.indexOf(a) >= SEVERITY_ORDER.indexOf(b) ? a : b;
    }

    public Optional<Incident> acknowledge(String id) {
        return incidentRepository.findById(id).map(incident -> {
            incident.setStatus(IncidentStatus.ACKNOWLEDGED);
            return incidentRepository.save(incident);
        });
    }

    public Optional<Incident> resolve(String id) {
        return incidentRepository.findById(id).map(incident -> {
            incident.setStatus(IncidentStatus.RESOLVED);
            Incident saved = incidentRepository.save(incident);
            notificationHub.dispatch(saved, IncidentEvent.RESOLVED);  // ← propage aux canaux (Jira)
            return saved;
        });
    }
}
