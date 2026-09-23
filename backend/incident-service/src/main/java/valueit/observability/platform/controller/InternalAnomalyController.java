package valueit.observability.platform.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import valueit.observability.platform.dto.AnomalyReport;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.service.IncidentService;

/**
 * Endpoint interne appelé par monitoring-service pour chaque anomalie détectée.
 * Non destiné aux clients externes (masqué dans Swagger).
 */
@Hidden
@RestController
@RequestMapping("/internal/anomalies")
public class InternalAnomalyController {

    private final IncidentService incidentService;

    public InternalAnomalyController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<Incident> report(@RequestBody AnomalyReport report) {
        return ResponseEntity.ok(incidentService.handle(report));
    }
}
