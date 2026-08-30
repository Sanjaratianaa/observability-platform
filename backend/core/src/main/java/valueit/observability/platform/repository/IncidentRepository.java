package valueit.observability.platform.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.incident.IncidentStatus;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends ElasticsearchRepository<Incident, String> {
    Optional<Incident> findByFingerprintAndStatus(String fingerprint, IncidentStatus status);
    List<Incident> findByStatus(IncidentStatus status);
    List<Incident> findBySeverity(Severity severity);
}
