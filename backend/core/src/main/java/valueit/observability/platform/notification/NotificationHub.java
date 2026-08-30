package valueit.observability.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentEvent;

import java.util.List;

@Service
public class NotificationHub {

    private static final Logger log = LoggerFactory.getLogger(NotificationHub.class);

    private final List<Notifier> notifiers;

    public NotificationHub(List<Notifier> notifiers) {
        this.notifiers = notifiers;
    }

    public void dispatch(Incident incident, IncidentEvent event) {
        for (Notifier notifier : notifiers) {
            if (!notifier.supports(incident)) {
                continue;
            }
            try {
                notifier.notify(incident, event);
            } catch (Exception e) {
                log.error("Notifier {} a échoué : {}", notifier.getName(), e.getMessage(), e);
            }
        }
    }
}
