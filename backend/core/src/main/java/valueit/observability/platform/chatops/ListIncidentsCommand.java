package valueit.observability.platform.chatops;

import org.springframework.stereotype.Component;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentStatus;
import valueit.observability.platform.repository.IncidentRepository;

import java.util.List;

@Component
public class ListIncidentsCommand implements ChatCommand {

    private final IncidentRepository incidentRepository;

    public ListIncidentsCommand(IncidentRepository incidentRepository) {
        this.incidentRepository = incidentRepository;
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
        List<Incident> open = incidentRepository.findByStatus(IncidentStatus.OPEN);
        if (open.isEmpty()) {
            return "Aucun incident ouvert.";
        }
        StringBuilder sb = new StringBuilder("Incidents ouverts (" + open.size() + ") :\n");
        for (Incident i : open) {
            sb.append("- ").append(i.getId())
                    .append(" [").append(i.getSeverity()).append("] ")
                    .append(i.getType()).append(" @ ").append(i.getSource())
                    .append(" (x").append(i.getOccurrenceCount()).append(")\n");
        }
        return sb.toString();
    }
}
