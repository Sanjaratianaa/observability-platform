package valueit.observability.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

/**
 * Client REST vers incident-service : les commandes ChatOps délèguent
 * la lecture et les transitions d'état des incidents à l'API distante.
 */
@Component
public class IncidentApiClient {

    private static final Logger log = LoggerFactory.getLogger(IncidentApiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IncidentApiClient(@Value("${incident.service.url:http://localhost:8082}") String incidentServiceUrl,
                             ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder()
                .baseUrl(incidentServiceUrl)
                .requestFactory(factory)
                .build();
    }

    public String listOpen() {
        try {
            JsonNode incidents = restClient.get()
                    .uri("/api/incidents?status=OPEN")
                    .retrieve()
                    .body(JsonNode.class);

            if (incidents == null || !incidents.isArray() || incidents.isEmpty()) {
                return "Aucun incident ouvert.";
            }

            StringBuilder sb = new StringBuilder("Incidents ouverts (" + incidents.size() + ") :\n");
            for (JsonNode i : incidents) {
                sb.append("- ").append(i.path("id").asText("?"))
                        .append(" [").append(i.path("severity").asText("?")).append("] ")
                        .append(i.path("type").asText("?")).append(" @ ")
                        .append(i.path("source").asText("?"))
                        .append(" (x").append(i.path("occurrenceCount").asInt(1)).append(")\n");
            }
            return sb.toString();
        } catch (Exception e) {
            return unreachable(e);
        }
    }

    public String stats() {
        try {
            JsonNode stats = restClient.get()
                    .uri("/api/incidents/stats")
                    .retrieve()
                    .body(JsonNode.class);

            if (stats == null) {
                return "Statistiques indisponibles.";
            }
            return "Stats incidents — OPEN: " + stats.path("OPEN").asLong(0)
                    + " | ACK: " + stats.path("ACKNOWLEDGED").asLong(0)
                    + " | RESOLVED: " + stats.path("RESOLVED").asLong(0);
        } catch (Exception e) {
            return unreachable(e);
        }
    }

    public String acknowledge(String id) {
        return transition(id, "ack", "acquitté");
    }

    public String resolve(String id) {
        return transition(id, "resolve", "résolu");
    }

    private String transition(String id, String action, String doneLabel) {
        try {
            JsonNode incident = restClient.put()
                    .uri("/api/incidents/" + id + "/" + action)
                    .retrieve()
                    .body(JsonNode.class);
            return "Incident " + (incident != null ? incident.path("id").asText(id) : id)
                    + " " + doneLabel + ".";
        } catch (HttpClientErrorException.NotFound e) {
            return "Incident introuvable : " + id;
        } catch (HttpClientErrorException e) {
            return "Erreur : " + extractMessage(e);
        } catch (Exception e) {
            return unreachable(e);
        }
    }

    private String extractMessage(HttpClientErrorException e) {
        try {
            JsonNode body = objectMapper.readTree(e.getResponseBodyAsString());
            String message = body.path("message").asText(null);
            if (message != null) {
                return message;
            }
        } catch (Exception ignored) {
            // corps non JSON ou sans champ message
        }
        return e.getStatusCode().toString();
    }

    private String unreachable(Exception e) {
        log.warn("incident-service injoignable : {}", e.getMessage());
        return "Erreur : incident-service injoignable (" + e.getMessage() + ")";
    }
}
