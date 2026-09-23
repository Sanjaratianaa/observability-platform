package valueit.observability.platform.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PlatformMetrics {

    private final Counter logsIngested;
    private final Counter logsParseErrors;
    private final Counter anomaliesDetected;

    public PlatformMetrics(MeterRegistry registry) {
        this.logsIngested = Counter.builder("obs.logs.ingested")
                .description("Total logs ingested")
                .register(registry);

        this.logsParseErrors = Counter.builder("obs.logs.parse_errors")
                .description("Log parsing failures")
                .register(registry);

        this.anomaliesDetected = Counter.builder("obs.anomalies.detected")
                .description("Anomalies detected")
                .register(registry);
    }

    public void logIngested() { logsIngested.increment(); }
    public void logParseError() { logsParseErrors.increment(); }
    public void anomalyDetected() { anomaliesDetected.increment(); }
}
