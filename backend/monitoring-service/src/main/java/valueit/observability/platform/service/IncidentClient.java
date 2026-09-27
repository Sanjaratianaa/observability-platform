package valueit.observability.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import valueit.observability.platform.dto.AnomalyReport;
import valueit.observability.platform.metrics.PlatformMetrics;
import valueit.observability.platform.outbox.AnomalyOutboxEntry;
import valueit.observability.platform.outbox.AnomalyOutboxRepository;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Client REST vers incident-service : transmet chaque anomalie détectée
 * pour corrélation, déduplication et notification.
 */
@Component
public class IncidentClient {

    private static final Logger log = LoggerFactory.getLogger(IncidentClient.class);

    private final RestClient restClient;
    private final PlatformMetrics metrics;
    private final AnomalyOutboxRepository outboxRepository;
    private final Executor incidentTaskExecutor;

    public IncidentClient(@Value("${incident.service.url:http://localhost:8082}") String incidentServiceUrl,
                          PlatformMetrics metrics,
                          AnomalyOutboxRepository outboxRepository,
                          Executor incidentTaskExecutor) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(incidentServiceUrl)
                .requestFactory(factory)
                .build();
        this.metrics = metrics;
        this.outboxRepository = outboxRepository;
        this.incidentTaskExecutor = incidentTaskExecutor;
    }

    @Async("incidentTaskExecutor")
    public void report(AnomalyReport anomalyReport) {
        reportBatch(List.of(anomalyReport));
    }

    public void reportBatch(List<AnomalyReport> anomalyReports) {
        if (anomalyReports.isEmpty()) {
            return;
        }
        outboxRepository.saveAll(anomalyReports.stream().map(AnomalyOutboxEntry::new).toList());
        incidentTaskExecutor.execute(this::drainOutbox);
    }

    @Scheduled(fixedDelayString = "${incident.outbox.poll-delay-ms:5000}")
    public void scheduleOutboxDrain() {
        incidentTaskExecutor.execute(this::drainOutbox);
    }

    private void drainOutbox() {
        List<AnomalyOutboxEntry> entries = outboxRepository.findTop100ByOrderByCreatedAtAsc();
        if (entries.isEmpty()) {
            return;
        }
        List<AnomalyReport> reports = entries.stream().map(AnomalyOutboxEntry::toReport).toList();
        Exception lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                restClient.post()
                        .uri("/internal/anomalies/bulk")
                        .body(reports)
                        .retrieve()
                        .toBodilessEntity();
                outboxRepository.deleteAll(entries);
                metrics.anomalyReportsSubmitted(reports.size());
                return;
            } catch (Exception e) {
                lastFailure = e;
                if (attempt < 3) {
                    metrics.anomalyReportRetried();
                    try {
                        Thread.sleep(250L * attempt);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        log.error("Transmission des anomalies interrompue", interrupted);
                        return;
                    }
                }
            }
        }

        log.error("Transmission de {} anomalie(s) impossible après 3 tentatives : {}",
                reports.size(), lastFailure == null ? "cause inconnue" : lastFailure.getMessage());
        metrics.anomalyReportsFailed(reports.size());
    }

    private void reportSingle(AnomalyReport anomalyReport) {
        try {
            restClient.post()
                    .uri("/internal/anomalies")
                    .body(anomalyReport)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Isolation de panne : l'ingestion ne doit pas échouer si incident-service est down
            log.error("Transmission de l'anomalie à incident-service impossible : {}", e.getMessage());
        }
    }
}
