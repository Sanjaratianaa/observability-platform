package valueit.observability.platform.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class PlatformMetrics {

    private final Counter logsIngested;
    private final Counter logsParseErrors;
    private final Counter anomaliesDetected;
    private final Counter anomalyReportsSubmitted;
    private final Counter anomalyReportsRetried;
    private final Counter anomalyReportsFailed;

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
        this.anomalyReportsSubmitted = Counter.builder("obs.anomaly_reports.submitted")
                .description("Anomaly batches accepted by incident-service")
                .register(registry);
        this.anomalyReportsRetried = Counter.builder("obs.anomaly_reports.retried")
                .description("Anomaly batch retry attempts")
                .register(registry);
        this.anomalyReportsFailed = Counter.builder("obs.anomaly_reports.failed")
                .description("Anomaly batches that failed after retries")
                .register(registry);
    }

    public void logIngested() { logsIngested.increment(); }
    public void logParseError() { logsParseErrors.increment(); }
    public void anomalyDetected() { anomaliesDetected.increment(); }
    public void anomalyReportsSubmitted(int count) { anomalyReportsSubmitted.increment(count); }
    public void anomalyReportRetried() { anomalyReportsRetried.increment(); }
    public void anomalyReportsFailed(int count) { anomalyReportsFailed.increment(count); }
}
