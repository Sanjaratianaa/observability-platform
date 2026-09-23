package valueit.observability.platform.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentEvent;
import valueit.observability.platform.metadata.NotificationRecord;
import valueit.observability.platform.metadata.NotificationRecordRepository;
import valueit.observability.platform.metrics.PlatformMetrics;

import java.util.List;

@Service
public class NotificationHub {

    private static final Logger log = LoggerFactory.getLogger(NotificationHub.class);

    private final List<Notifier> notifiers;
    private final NotificationRecordRepository recordRepository;
    private final PlatformMetrics metrics;

    public NotificationHub(List<Notifier> notifiers,
                           NotificationRecordRepository recordRepository,
                           PlatformMetrics metrics) {
        this.notifiers = notifiers;
        this.recordRepository = recordRepository;
        this.metrics = metrics;
    }

    @Async
    public void dispatch(Incident incident, IncidentEvent event) {
        for (Notifier notifier : notifiers) {
            if (!notifier.supports(incident)) {
                continue;
            }
            NotificationRecord record = new NotificationRecord();
            record.setChannel(channelOf(notifier));
            record.setIncidentId(incident.getId());
            record.setEventType(event.name());
            record.setExternalRef(incident.getJiraTicketKey());
            try {
                notifier.notify(incident, event);
                record.setSuccess(true);
                metrics.notificationSent();
            } catch (Exception e) {
                record.setSuccess(false);
                record.setErrorMessage(e.getMessage());
                metrics.notificationFailed();
                log.error("Notifier {} a échoué : {}", notifier.getName(), e.getMessage(), e);
            }
            try {
                recordRepository.save(record);
            } catch (Exception e) {
                log.warn("Impossible de persister le NotificationRecord : {}", e.getMessage());
            }
        }
    }

    private String channelOf(Notifier notifier) {
        return notifier.getName().replace("Notifier", "").toUpperCase();
    }
}
