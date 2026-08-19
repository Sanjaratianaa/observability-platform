package valueit.observability.platform.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import valueit.observability.platform.incident.Incident;

import java.util.Base64;
import java.nio.charset.StandardCharsets;

@Component
public class JiraNotifier implements Notifier {

    private final String baseUrl;
    private final String email;
    private final String apiToken;
    private final String projectKey;
    private final RestClient restClient;

    public JiraNotifier(
            @Value("${notification.jira.base-url}") String baseUrl,
            @Value("${notification.jira.email}") String email,
            @Value("${notification.jira.api-token}") String apiToken,
            @Value("${notification.jira.project-key}") String projectKey) {
        this.baseUrl = baseUrl;
        this.email = email;
        this.apiToken = apiToken;
        this.projectKey = projectKey;
        this.restClient = RestClient.create();
    }

    @Override
    public boolean supports(Incident incident) {
        // ROUTAGE : on ne crée un ticket QUE pour les incidents graves.
        // Les LOW/MEDIUM vont sur Teams mais n'encombrent pas Jira.
        String s = incident.getSeverity();
        return "HIGH".equals(s) || "CRITICAL".equals(s);
    }

    @Override
    public void notify(Incident incident) {
        if (baseUrl.contains("not-configured")) {
            System.out.println("[Jira] Non configuré, création de ticket ignorée.");
            return;
        }

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

            System.out.println("[Jira] Ticket créé pour l'incident " + incident.getType()
                    + " → " + response);
            // TODO (mise à jour) : parser la clé du ticket dans la réponse
            //  et faire incident.setJiraTicketKey(...) + persister.
        } catch (Exception e) {
            System.err.println("[Jira] Erreur création ticket : " + e.getMessage());
        }
    }

    private String basicAuth() {
        String credentials = email + ":" + apiToken;
        return Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }
}
