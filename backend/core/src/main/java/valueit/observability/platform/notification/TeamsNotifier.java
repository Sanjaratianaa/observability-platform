package valueit.observability.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentEvent;
import valueit.observability.platform.incident.Severity;

@Component
public class TeamsNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(TeamsNotifier.class);

    private final RestClient restClient;
    private final String webhookUrl;

    public TeamsNotifier(@Value("${notification.teams.webhook-url}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.create();
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

        String payload = """
                {
                  "@type": "MessageCard",
                  "themeColor": "%s",
                  "title": "🚨 %s",
                  "text": "%s",
                  "sections": [{
                    "facts": [
                      {"name": "Sévérité", "value": "%s"},
                      {"name": "Source", "value": "%s"},
                      {"name": "Occurrences", "value": "%d"},
                      {"name": "Première vue", "value": "%s"}
                    ]
                  }]
                }
                """.formatted(
                severityToColor(incident.getSeverity()),
                incident.getType(),
                incident.getDescription().replace("\"", "'"),
                incident.getSeverity(),
                incident.getSource(),
                incident.getOccurrenceCount(),
                String.valueOf(incident.getFirstSeen())
        );

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
