package valueit.observability.platform.chatops;

import org.springframework.stereotype.Component;
import valueit.observability.platform.incident.IncidentStatus;
import valueit.observability.platform.repository.IncidentRepository;

@Component
public class StatsCommand implements  ChatCommand {

    private final IncidentRepository incidentRepository;

    public StatsCommand(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
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
        long open = incidentRepository.findByStatus(IncidentStatus.OPEN).size();
        long ack = incidentRepository.findByStatus(IncidentStatus.ACKNOWLEDGED).size();
        long resolved = incidentRepository.findByStatus(IncidentStatus.RESOLVED).size();
        return "Stats incidents — OPEN: " + open + " | ACK: " + ack + " | RESOLVED: " + resolved;
    }
}
