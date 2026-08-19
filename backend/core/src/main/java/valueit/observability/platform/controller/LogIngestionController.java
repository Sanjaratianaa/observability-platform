package valueit.observability.platform.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import valueit.observability.platform.anomaly.Anomaly;
import valueit.observability.platform.anomaly.IncidentDeduplicator;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.notification.TeamsNotifier;
import valueit.observability.platform.repository.LogEntryRepository;
import valueit.observability.platform.service.*;

import java.util.List;

@RestController
@RequestMapping("/api/logs")
public class LogIngestionController {

    private final LogEntryRepository logEntryRepository;
    private final LogParsingService logParsingService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final IncidentDeduplicator incidentDeduplicator;
    private final TeamsNotifier teamsNotifier;

    public LogIngestionController(LogEntryRepository logEntryRepository,
                                  LogParsingService logParsingService,
                                  AnomalyDetectionService anomalyDetectionService,
                                  IncidentDeduplicator incidentDeduplicator,
                                  TeamsNotifier teamsNotifier) {
        this.logEntryRepository = logEntryRepository;
        this.logParsingService = logParsingService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.incidentDeduplicator = incidentDeduplicator;
        this.teamsNotifier = teamsNotifier;
    }

    @PostMapping("/raw")
    public ResponseEntity<LogEntry> ingestRaw(@RequestBody String rawLog) {
        LogEntry entry = logParsingService.parse(rawLog);
        LogEntry saved = logEntryRepository.save(entry);

        List<Anomaly> anomalies = anomalyDetectionService.analyze(saved);
        List<Anomaly> newAnomalies = anomalies.stream()
                .filter(incidentDeduplicator::isNew)
                .toList();

        if (!newAnomalies.isEmpty()) {
            newAnomalies.forEach(a -> {
                System.out.println("NOUVEL INCIDENT : " + a.getDescription() + " [" + a.getSeverity() + "]");
                teamsNotifier.send(a);
            });
            // TODO : ici on déclenchera Jira
        }

        return ResponseEntity.ok(saved);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LogEntry ingest(@RequestBody LogEntry logEntry) {
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(java.time.Instant.now());
        }
        return logEntryRepository.save(logEntry);
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<LogEntry> ingestBulk(@RequestBody List<LogEntry> logs) {
        logs.forEach(log -> {
            if (log.getTimestamp() == null) {
                log.setTimestamp(java.time.Instant.now());
            }
        });
        return (List<LogEntry>) logEntryRepository.saveAll(logs);
    }

    @GetMapping
    public List<LogEntry> getAll() {
        return (List<LogEntry>) logEntryRepository.findAll();
    }

    @GetMapping("/level/{level}")
    public List<LogEntry> getByLevel(@PathVariable String level) {
        return logEntryRepository.findByLevel(level);
    }
}
