package valueit.observability.platform.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PlatformMetrics {

    private final Counter incidentsCreated;
    private final Counter incidentsResolved;
    private final Counter notificationsSent;
    private final Counter notificationsFailed;

    public PlatformMetrics(MeterRegistry registry) {
        this.incidentsCreated = Counter.builder("obs.incidents.created")
                .description("Incidents created")
                .register(registry);

        this.incidentsResolved = Counter.builder("obs.incidents.resolved")
                .description("Incidents resolved")
                .register(registry);

        this.notificationsSent = Counter.builder("obs.notifications.sent")
                .description("Notifications sent successfully")
                .register(registry);

        this.notificationsFailed = Counter.builder("obs.notifications.failed")
                .description("Notifications that failed")
                .register(registry);
    }

    public void incidentCreated() { incidentsCreated.increment(); }
    public void incidentResolved() { incidentsResolved.increment(); }
    public void notificationSent() { notificationsSent.increment(); }
    public void notificationFailed() { notificationsFailed.increment(); }
}
