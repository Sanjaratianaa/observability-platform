package valueit.observability.platform.chatops;

import org.springframework.stereotype.Component;
import valueit.observability.platform.service.IncidentApiClient;

@Component
public class ListIncidentsCommand implements ChatCommand {

    private final IncidentApiClient incidentApiClient;

    public ListIncidentsCommand(IncidentApiClient incidentApiClient) {
        this.incidentApiClient = incidentApiClient;
    }

    @Override
    public String name() {
        return "list";
    }

    @Override
    public String help() {
        return "list — liste les incidents ouverts";
    }

    @Override
    public String execute(String[] args) {
        return incidentApiClient.listOpen();
    }
}
