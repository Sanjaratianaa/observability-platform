package valueit.observability.platform.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentEvent;
import valueit.observability.platform.repository.IncidentRepository;

import java.util.Base64;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class JiraNotifier implements Notifier {

    private final String baseUrl;
    private final String email;
    private final String apiToken;
    private final String projectKey;
    private final RestClient restClient;
    private final IncidentRepository incidentRepository;
    private final ObjectMapper objectMapper;

    public JiraNotifier(
            @Value("${notification.jira.base-url}") String baseUrl,
            @Value("${notification.jira.email}") String email,
            @Value("${notification.jira.api-token}") String apiToken,
            @Value("${notification.jira.project-key}") String projectKey,
            IncidentRepository incidentRepository,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.email = email;
        this.apiToken = apiToken;
        this.projectKey = projectKey;
        this.incidentRepository = incidentRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @Override
    public boolean supports(Incident incident) {
        String s = incident.getSeverity();
        return "HIGH".equals(s) || "CRITICAL".equals(s);
    }

    @Override
    public void notify(Incident incident, IncidentEvent event) {
        if (baseUrl.contains("not-configured")) {
            System.out.println("[Jira] Non configuré, action ignorée.");
            return;
        }

        if (incident.getJiraTicketKey() == null) {
            if (event == IncidentEvent.CREATED) {
                createTicket(incident);
            }
            return;
        }

        // IDEMPOTENCE : ticket déjà existant → on commente. Sinon → on crée.
        String message = switch (event) {
            case RESOLVED -> "✅ Incident résolu via ChatOps.";
            case RECURRED -> "🔁 Récurrence — occurrence n°" + incident.getOccurrenceCount()
                    + " (dernière vue : " + incident.getLastSeen() + ")";
            case CREATED  -> "Incident re-signalé.";
        };
        addComment(incident, message);
    }

    private void addComment(Incident incident, String message) {
        String body = """
            { "body": "%s" }
            """.formatted(message.replace("\"", "'"));

        try {
            restClient.post()
                    .uri(baseUrl + "/rest/api/2/issue/" + incident.getJiraTicketKey() + "/comment")
                    .header("Authorization", "Basic " + basicAuth())
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            System.out.println("[Jira] Commentaire ajouté à " + incident.getJiraTicketKey());
        } catch (Exception e) {
            System.err.println("[Jira] Erreur ajout commentaire : " + e.getMessage());
        }
    }

    private void createTicket(Incident incident) {
        String summary = "[" + incident.getSeverity() + "] "
                + incident.getType() + " sur " + incident.getSource();

        String payload = """
                {
                  "fields": {
                    "project": { "key": "%s" },
                    "summary": "%s",
                    "description": "%s",
                    "issuetype": { "name": "Bug" }
                  }
                }
                """.formatted(
                projectKey,
                summary.replace("\"", "'"),
                incident.getDescription().replace("\"", "'")
        );

        try {
            String response = restClient.post()
                    .uri(baseUrl + "/rest/api/2/issue")
                    .header("Authorization", "Basic " + basicAuth())
                    .header("Content-Type", "application/json")
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            String key = objectMapper.readTree(response).path("key").asText(null);
            if (key != null) {
                incident.setJiraTicketKey(key);
                incidentRepository.save(incident);   // ← on MÉMORISE la clé
            }
            System.out.println("[Jira] Ticket créé : " + key);
        } catch (Exception e) {
            System.err.println("[Jira] Erreur création ticket : " + e.getMessage());
        }
    }

    private void addComment(Incident incident) {
        String body = """
                { "body": "Récurrence détectée — occurrence n°%d (dernière vue : %s)" }
                """.formatted(incident.getOccurrenceCount(),
                String.valueOf(incident.getLastSeen()));

        try {
            restClient.post()
                    .uri(baseUrl + "/rest/api/2/issue/" + incident.getJiraTicketKey() + "/comment")
                    .header("Authorization", "Basic " + basicAuth())
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            System.out.println("[Jira] Commentaire ajouté à " + incident.getJiraTicketKey());
        } catch (Exception e) {
            System.err.println("[Jira] Erreur ajout commentaire : " + e.getMessage());
        }
    }

    private String basicAuth() {
        String credentials = email + ":" + apiToken;
        return Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
