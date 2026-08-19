package valueit.observability.platform.notification;

import valueit.observability.platform.incident.Incident;

public interface Notifier {
    boolean supports(Incident incident);
    void notify(Incident incident);
    default String getName(){
        return getClass().getSimpleName();
    }
}
