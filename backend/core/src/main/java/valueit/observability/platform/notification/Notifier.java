package valueit.observability.platform.notification;

import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentEvent;

public interface Notifier {
    boolean supports(Incident incident);
    void notify(Incident incident, IncidentEvent event);
    default String getName(){
        return getClass().getSimpleName();
    }
}
