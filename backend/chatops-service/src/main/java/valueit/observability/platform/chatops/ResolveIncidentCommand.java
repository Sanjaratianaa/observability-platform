package valueit.observability.platform.chatops;

import org.springframework.stereotype.Component;
import valueit.observability.platform.service.IncidentApiClient;

@Component
public class ResolveIncidentCommand implements ChatCommand {

    private final IncidentApiClient incidentApiClient;

    public ResolveIncidentCommand(IncidentApiClient incidentApiClient) {
        this.incidentApiClient = incidentApiClient;
    }

    @Override
    public String name() {
        return "resolve";
    }

    @Override
    public String help() {
        return "resolve <id> — clôture un incident (→ RESOLVED) + notifie Jira";
    }

    @Override
    public String execute(String[] args) {
        if (args.length < 1) {
            return "Usage : resolve <id>";
        }
        return incidentApiClient.resolve(args[0]);
    }
}
