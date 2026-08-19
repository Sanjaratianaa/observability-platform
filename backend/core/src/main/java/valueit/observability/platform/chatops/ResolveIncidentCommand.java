package valueit.observability.platform.chatops;

import org.springframework.stereotype.Component;
import valueit.observability.platform.service.IncidentService;

@Component
public class ResolveIncidentCommand implements ChatCommand {

    private final IncidentService incidentService;

    public ResolveIncidentCommand(IncidentService incidentService) {
        this.incidentService = incidentService;
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
        return incidentService.resolve(args[0])
                .map(incident -> "Incident " + incident.getId() + " résolu.")
                .orElse("Incident introuvable : " + args[0]);
    }
}