package valueit.observability.platform.chatops;

import org.springframework.stereotype.Component;
import valueit.observability.platform.service.IncidentService;

@Component
public class AckIncidentCommand implements ChatCommand {

    private final IncidentService incidentService;

    public AckIncidentCommand(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @Override
    public String name() {
        return "ack";
    }

    @Override
    public String help() {
        return "ack <id> — acquitte un incident (OPEN → ACKNOWLEDGED)";
    }

    @Override
    public String execute(String[] args) {
        if (args.length < 1) {
            return "Usage : ack <id>";
        }
        return incidentService.acknowledge(args[0])
                .map(incident -> "Incident " + incident.getId() + " acquitté.")
                .orElse("Incident introuvable : " + args[0]);
    }
}