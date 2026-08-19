package valueit.observability.platform.service;

import org.springframework.stereotype.Service;
import valueit.observability.platform.anomaly.Anomaly;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentStatus;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.notification.NotificationHub;
import valueit.observability.platform.repository.IncidentRepository;

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
        // THROTTLING : une récurrence ne re-notifie PAS (évite le spam).
        // (plus tard : re-notifier seulement si la sévérité a escaladé)
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
        notificationHub.dispatch(saved);   // NOUVEL incident → on notifie tous les canaux compatibles
        return saved;
    }

    private String buildFingerprint(Anomaly anomaly, LogEntry sourceLog) {
        return anomaly.getType() + "::" + sourceLog.getSource();
    }

    private String maxSeverity(String a, String b) {
        return SEVERITY_ORDER.indexOf(a) >= SEVERITY_ORDER.indexOf(b) ? a : b;
    }
}
