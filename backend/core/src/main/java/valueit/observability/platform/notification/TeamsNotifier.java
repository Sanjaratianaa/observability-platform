package valueit.observability.platform.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentEvent;

@Component
public class TeamsNotifier implements Notifier {

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
            System.out.println("[Teams] Webhook non configuré, notification ignorée.");
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
            System.out.println("[Teams] Notification envoyée : " + incident.getType());
        } catch (Exception e) {
            System.err.println("[Teams] Erreur d'envoi : " + e.getMessage());
        }
    }

    private String severityToColor(String severity) {
        return switch (severity) {
            case "CRITICAL" -> "FF0000";
            case "HIGH" -> "FF6600";
            case "MEDIUM" -> "FFAA00";
            default -> "00AA00";
        };
    }
}
