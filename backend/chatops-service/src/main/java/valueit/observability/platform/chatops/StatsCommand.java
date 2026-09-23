package valueit.observability.platform.chatops;

import org.springframework.stereotype.Component;
import valueit.observability.platform.service.IncidentApiClient;

@Component
public class StatsCommand implements ChatCommand {

    private final IncidentApiClient incidentApiClient;

    public StatsCommand(IncidentApiClient incidentApiClient) {
        this.incidentApiClient = incidentApiClient;
    }

    @Override
    public String name() {
        return "stats";
    }

    @Override
    public String help() {
        return "stats — compte les incidents par statut";
    }

    @Override
    public String execute(String[] args) {
        return incidentApiClient.stats();
    }
}
