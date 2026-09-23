package valueit.observability.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentEvent;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.repository.IncidentRepository;

import java.time.Duration;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public class JiraNotifier implements Notifier {

    private static final Logger log = LoggerFactory.getLogger(JiraNotifier.class);

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
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    @Override
    public boolean supports(Incident incident) {
        Severity s = incident.getSeverity();
        return s == Severity.HIGH || s == Severity.CRITICAL;
    }

    @Override
    public void notify(Incident incident, IncidentEvent event) {
        if (baseUrl.contains("not-configured")) {
            log.warn("Jira non configuré, action ignorée");
            return;
        }

        if (incident.getJiraTicketKey() == null) {
            // supports() garantit déjà HIGH/CRITICAL : on crée le ticket même sur RECURRED
            // (la sévérité a pu être escaladée après la création de l'incident)
            if (event != IncidentEvent.RESOLVED) {
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
        ObjectNode body = objectMapper.createObjectNode();
        body.put("body", message);

        try {
            restClient.post()
                    .uri(baseUrl + "/rest/api/2/issue/" + incident.getJiraTicketKey() + "/comment")
                    .header("Authorization", "Basic " + basicAuth())
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Commentaire Jira ajouté à {}", incident.getJiraTicketKey());
        } catch (Exception e) {
            log.error("Erreur ajout commentaire Jira : {}", e.getMessage(), e);
        }
    }

    private void createTicket(Incident incident) {
        String summary = "[" + incident.getSeverity() + "] "
                + incident.getType() + " sur " + incident.getSource();

        ObjectNode fields = objectMapper.createObjectNode();
        fields.putObject("project").put("key", projectKey);
        fields.put("summary", summary);
        fields.put("description", incident.getDescription());
        fields.putObject("issuetype").put("name", "Bug");
        ObjectNode payload = objectMapper.createObjectNode();
        payload.set("fields", fields);

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
                incidentRepository.save(incident);
            }
            log.info("Ticket Jira créé : {}", key);
        } catch (Exception e) {
            log.error("Erreur création ticket Jira : {}", e.getMessage(), e);
        }
    }

    private String basicAuth() {
        String credentials = email + ":" + apiToken;
        return Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
