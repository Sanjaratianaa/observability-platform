package valueit.observability.platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentStatus;
import valueit.observability.platform.repository.IncidentRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;

    public IncidentRepository(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
    }

    @GetMapping
    public List<Incident> list(@RequestParam(required = false) IncidentStatus status,
                               @RequestParam(required = false) String severity) {
        if (status != null) {
            return incidentRepository.findByStatus(status);
        }
        if (severity != null) {
            return incidentRepository.findBySeverity(severity.toUpperCase());
        }
        return (List<Incident>) incidentRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getOne(@PathVariable String id) {
        return incidentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (IncidentStatus s : IncidentStatus.values()) {
            counts.put(s.name(), (long) incidentRepository.findByStatus(s).size());
        }
        return counts;
    }
}
