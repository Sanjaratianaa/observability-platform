package valueit.observability.platform.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import valueit.observability.platform.anomaly.Anomaly;

@Component
public class TeamsNotifier {

    private final RestClient restClient;
    private final String webhookUrl;

    public TeamsNotifier(@Value("${notification.teams.webhook-url}") String webhookUrl) {
        this.webhookUrl = webhookUrl;
        this.restClient = RestClient.create();
    }

    public void send(Anomaly anomaly) {
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
                      {"name": "Type", "value": "%s"},
                      {"name": "Détecté à", "value": "%s"}
                    ]
                  }]
                }
                """.formatted(
                severityToColor(anomaly.getSeverity()),
                anomaly.getType(),
                anomaly.getDescription().replace("\"", "'"),
                anomaly.getSeverity(),
                anomaly.getType(),
                anomaly.getDetectedAt().toString()
        );

        try {
            restClient.post()
                    .uri(webhookUrl)
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            System.out.println("[Teams] Notification envoyée : " + anomaly.getType());
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
