package valueit.observability.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentEvent;
import valueit.observability.platform.incident.Severity;

import java.time.Duration;

@Component
public class TeamsNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(TeamsNotifier.class);

    private final RestClient restClient;
    private final String webhookUrl;
    private final ObjectMapper objectMapper;

    public TeamsNotifier(@Value("${notification.teams.webhook-url}") String webhookUrl,
                         ObjectMapper objectMapper) {
        this.webhookUrl = webhookUrl;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean supports(Incident incident) {
        return true;
    }


    @Override
    public void notify(Incident incident, IncidentEvent event) {
        if (event != IncidentEvent.CREATED) {
            return;
        }

        if (webhookUrl.contains("not-configured")) {
            log.warn("Teams webhook non configuré, notification ignorée");
            return;
        }

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("@type", "MessageCard");
        payload.put("themeColor", severityToColor(incident.getSeverity()));
        payload.put("title", "🚨 " + incident.getType());
        payload.put("text", incident.getDescription());
        var facts = payload.putArray("sections").addObject().putArray("facts");
        facts.addObject().put("name", "Sévérité").put("value", String.valueOf(incident.getSeverity()));
        facts.addObject().put("name", "Source").put("value", incident.getSource());
        facts.addObject().put("name", "Occurrences").put("value", String.valueOf(incident.getOccurrenceCount()));
        facts.addObject().put("name", "Première vue").put("value", String.valueOf(incident.getFirstSeen()));

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Notification Teams envoyée : {}", incident.getType());
        } catch (Exception e) {
            log.error("Erreur envoi Teams : {}", e.getMessage(), e);
        }
    }

    private String severityToColor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "FF0000";
            case HIGH -> "FF6600";
            case MEDIUM -> "FFAA00";
            case LOW -> "00AA00";
        };
    }
}
