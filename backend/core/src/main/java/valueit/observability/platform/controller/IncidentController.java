package valueit.observability.platform.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentStatus;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.repository.IncidentRepository;
import valueit.observability.platform.service.IncidentService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentRepository incidentRepository;
    private final IncidentService incidentService;

    public IncidentController(IncidentRepository incidentRepository, IncidentService incidentService) {
        this.incidentRepository = incidentRepository;
        this.incidentService = incidentService;
    }

    @GetMapping
    public List<Incident> list(@RequestParam(required = false) IncidentStatus status,
                               @RequestParam(required = false) Severity severity) {
        if (status != null) {
            return incidentRepository.findByStatus(status);
        }
        if (severity != null) {
            return incidentRepository.findBySeverity(severity);
        }
        return StreamSupport.stream(incidentRepository.findAll().spliterator(), false).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Incident> getOne(@PathVariable String id) {
        return incidentRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/ack")
    public ResponseEntity<Incident> acknowledge(@PathVariable String id) {
        return incidentService.acknowledge(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<Incident> resolve(@PathVariable String id) {
        return incidentService.resolve(id)
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
