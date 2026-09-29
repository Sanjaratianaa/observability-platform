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

        ObjectNode payload = buildPayload(incident);

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

    /**
     * Workflows / Power Automate webhooks (logic.azure.com) require an
     * Adaptive Card payload. Legacy Office connectors (webhook.office.com)
     * use MessageCard.
     */
    private ObjectNode buildPayload(Incident incident) {
        if (webhookUrl.contains("logic.azure.com") || webhookUrl.contains("powerautomate")) {
            return buildAdaptiveCard(incident);
        }
        return buildMessageCard(incident);
    }

    private ObjectNode buildMessageCard(Incident incident) {
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
        return payload;
    }

    private ObjectNode buildAdaptiveCard(Incident incident) {
        var body = objectMapper.createArrayNode();

        var title = objectMapper.createObjectNode();
        title.put("type", "TextBlock");
        title.put("text", "🚨 " + incident.getType());
        title.put("weight", "Bolder");
        title.put("size", "Large");
        title.put("color", severityToAdaptiveColor(incident.getSeverity()));
        body.add(title);

        var desc = objectMapper.createObjectNode();
        desc.put("type", "TextBlock");
        desc.put("text", incident.getDescription());
        desc.put("wrap", true);
        body.add(desc);

        var factSet = objectMapper.createObjectNode();
        factSet.put("type", "FactSet");
        var facts = factSet.putArray("facts");
        facts.addObject().put("title", "Sévérité").put("value", String.valueOf(incident.getSeverity()));
        facts.addObject().put("title", "Source").put("value", incident.getSource());
        facts.addObject().put("title", "Occurrences").put("value", String.valueOf(incident.getOccurrenceCount()));
        facts.addObject().put("title", "Première vue").put("value", String.valueOf(incident.getFirstSeen()));
        body.add(factSet);

        ObjectNode card = objectMapper.createObjectNode();
        card.put("$schema", "http://adaptivecards.io/schemas/adaptive-card.json");
        card.put("type", "AdaptiveCard");
        card.put("version", "1.4");
        card.set("body", body);

        ObjectNode attachment = objectMapper.createObjectNode();
        attachment.put("contentType", "application/vnd.microsoft.card.adaptive");
        attachment.set("content", card);

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("type", "message");
        payload.putArray("attachments").add(attachment);
        return payload;
    }

    private String severityToColor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "FF0000";
            case HIGH -> "FF6600";
            case MEDIUM -> "FFAA00";
            case LOW -> "00AA00";
        };
    }

    private String severityToAdaptiveColor(Severity severity) {
        return switch (severity) {
            case CRITICAL -> "Attention";
            case HIGH -> "Warning";
            case MEDIUM -> "Accent";
            case LOW -> "Good";
        };
    }
}
