package valueit.observability.platform.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.repository.LogEntryRepository;
import valueit.observability.platform.service.*;

import java.util.List;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping("/api/logs")
public class LogIngestionController {

    private final LogEntryRepository logEntryRepository;
    private final LogParsingService logParsingService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final IncidentService incidentService;

    public LogIngestionController(LogEntryRepository logEntryRepository,
                                  LogParsingService logParsingService,
                                  AnomalyDetectionService anomalyDetectionService,
                                  IncidentService incidentService) {
        this.logEntryRepository = logEntryRepository;
        this.logParsingService = logParsingService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.incidentService = incidentService;
    }

    @PostMapping("/raw")
    public ResponseEntity<LogEntry> ingestRaw(@RequestBody String rawLog) {
        LogEntry entry = logParsingService.parse(rawLog);
        LogEntry saved = logEntryRepository.save(entry);

        anomalyDetectionService.analyze(saved)
                .forEach(anomaly -> incidentService.handle(anomaly, saved));

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
        return StreamSupport.stream(logEntryRepository.saveAll(logs).spliterator(), false).toList();
    }

    @GetMapping
    public Page<LogEntry> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return logEntryRepository.findAll(pageable);
    }

    @GetMapping("/level/{level}")
    public List<LogEntry> getByLevel(@PathVariable String level) {
        return logEntryRepository.findByLevel(level);
    }

    @GetMapping("/search")
    public List<LogEntry> search(
            @RequestParam java.time.Instant from,
            @RequestParam java.time.Instant to,
            @RequestParam(required = false) String level) {
        if (level != null) {
            return logEntryRepository.findByLevelAndTimestampBetween(level.toUpperCase(), from, to);
        }
        return logEntryRepository.findByTimestampBetween(from, to);
    }
}
