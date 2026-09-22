package valueit.observability.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import valueit.observability.platform.anomaly.Anomaly;
import valueit.observability.platform.metrics.PlatformMetrics;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.parser.LogParserException;
import valueit.observability.platform.repository.LogEntryRepository;
import valueit.observability.platform.service.*;

import java.util.List;
import java.util.stream.StreamSupport;

@Tag(name = "Logs", description = "Ingestion, consultation et recherche de logs")
@RestController
@RequestMapping("/api/logs")
public class LogIngestionController {

    private final LogEntryRepository logEntryRepository;
    private final LogParsingService logParsingService;
    private final AnomalyDetectionService anomalyDetectionService;
    private final IncidentService incidentService;
    private final PlatformMetrics metrics;

    public LogIngestionController(LogEntryRepository logEntryRepository,
                                  LogParsingService logParsingService,
                                  AnomalyDetectionService anomalyDetectionService,
                                  IncidentService incidentService,
                                  PlatformMetrics metrics) {
        this.logEntryRepository = logEntryRepository;
        this.logParsingService = logParsingService;
        this.anomalyDetectionService = anomalyDetectionService;
        this.incidentService = incidentService;
        this.metrics = metrics;
    }

    @Operation(summary = "Ingérer un log brut", description = "Parse automatiquement (JSON, Apache, Syslog) et déclenche la détection d'anomalies")
    @PostMapping("/raw")
    public ResponseEntity<LogEntry> ingestRaw(@RequestBody String rawLog) {
        LogEntry entry = logParsingService.parse(rawLog);
        LogEntry saved = logEntryRepository.save(entry);
        metrics.logIngested();

        anomalyDetectionService.analyze(saved)
                .forEach(anomaly -> {
                    metrics.anomalyDetected();
                    incidentService.handle(anomaly, saved);
                });

        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Ingérer des logs bruts en lot",
            description = "Une ligne = un log brut (text/plain). Les lignes vides et les commentaires (#) sont ignorés. " +
                    "Le paramètre ?source= force la source de tous les logs (ex: nom du fichier injecté).")
    @PostMapping(value = "/raw/bulk", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<BulkIngestResult> ingestRawBulk(
            @Parameter(description = "Source forcée pour tous les logs du lot") @RequestParam(required = false) String source,
            @RequestBody String body) {
        int received = 0;
        int ingested = 0;
        int parseErrors = 0;
        int anomaliesDetected = 0;

        for (String line : body.split("\\R")) {
            String raw = line.trim();
            if (raw.isEmpty() || raw.startsWith("#")) {
                continue;
            }
            received++;
            try {
                LogEntry entry = logParsingService.parse(raw);
                if (source != null && !source.isBlank()) {
                    entry.setSource(source);
                }
                LogEntry saved = logEntryRepository.save(entry);
                ingested++;
                metrics.logIngested();

                List<Anomaly> detected = anomalyDetectionService.analyze(saved);
                anomaliesDetected += detected.size();
                detected.forEach(anomaly -> {
                    metrics.anomalyDetected();
                    incidentService.handle(anomaly, saved);
                });
            } catch (LogParserException e) {
                parseErrors++;
                metrics.logParseError();
            }
        }

        return ResponseEntity.ok(new BulkIngestResult(received, ingested, parseErrors, anomaliesDetected));
    }

    public record BulkIngestResult(int received, int ingested, int parseErrors, int anomaliesDetected) {}

    @Operation(summary = "Ingérer un log structuré")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LogEntry ingest(@RequestBody LogEntry logEntry) {
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(java.time.Instant.now());
        }
        return logEntryRepository.save(logEntry);
    }

    @Operation(summary = "Ingérer plusieurs logs en lot")
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

    @Operation(summary = "Lister les logs", description = "Retourne les logs paginés, triés par timestamp décroissant")
    @GetMapping
    public Page<LogEntry> getAll(
            @Parameter(description = "Numéro de page (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return logEntryRepository.findAll(pageable);
    }

    @Operation(summary = "Filtrer les logs par niveau")
    @GetMapping("/level/{level}")
    public List<LogEntry> getByLevel(@Parameter(description = "Niveau : ERROR, WARN, INFO, DEBUG") @PathVariable String level) {
        return logEntryRepository.findByLevel(level);
    }

    @Operation(summary = "Recherche temporelle", description = "Recherche les logs dans une plage de dates, avec filtre optionnel par niveau")
    @GetMapping("/search")
    public List<LogEntry> search(
            @Parameter(description = "Début (ISO-8601)") @RequestParam java.time.Instant from,
            @Parameter(description = "Fin (ISO-8601)") @RequestParam java.time.Instant to,
            @Parameter(description = "Niveau optionnel") @RequestParam(required = false) String level) {
        if (level != null) {
            return logEntryRepository.findByLevelAndTimestampBetween(level.toUpperCase(), from, to);
        }
        return logEntryRepository.findByTimestampBetween(from, to);
    }
}
