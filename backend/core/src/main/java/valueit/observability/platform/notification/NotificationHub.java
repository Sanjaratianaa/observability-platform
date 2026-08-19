package valueit.observability.platform.notification;

import org.springframework.stereotype.Service;
import valueit.observability.platform.incident.Incident;

import java.util.List;

@Service
public class NotificationHub {

    private final List<Notifier> notifiers;

    public NotificationHub(List<Notifier> notifiers) {
        this.notifiers = notifiers;
    }

    public void dispatch(Incident incident) {
        for (Notifier notifier : notifiers) {
            if (!notifier.supports(incident)) {
                continue;
            }
            try {
                notifier.notify(incident);
            } catch (Exception e) {
                System.err.println("[NotificationHub] " + notifier.getName()
                        + " a échoué : " + e.getMessage());
            }
        }
    }
}
