package valueit.observability.platform.chatops;

import org.springframework.stereotype.Component;
import valueit.observability.platform.service.IncidentApiClient;

@Component
public class AckIncidentCommand implements ChatCommand {

    private final IncidentApiClient incidentApiClient;

    public AckIncidentCommand(IncidentApiClient incidentApiClient) {
        this.incidentApiClient = incidentApiClient;
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
        return incidentApiClient.acknowledge(args[0]);
    }
}
