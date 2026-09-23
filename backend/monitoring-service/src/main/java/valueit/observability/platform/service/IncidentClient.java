package valueit.observability.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import valueit.observability.platform.dto.AnomalyReport;

import java.time.Duration;

/**
 * Client REST vers incident-service : transmet chaque anomalie détectée
 * pour corrélation, déduplication et notification.
 */
@Component
public class IncidentClient {

    private static final Logger log = LoggerFactory.getLogger(IncidentClient.class);

    private final RestClient restClient;

    public IncidentClient(@Value("${incident.service.url:http://localhost:8082}") String incidentServiceUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(incidentServiceUrl)
                .requestFactory(factory)
                .build();
    }

    public void report(AnomalyReport anomalyReport) {
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
