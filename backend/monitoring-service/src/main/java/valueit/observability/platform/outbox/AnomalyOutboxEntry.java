package valueit.observability.platform.outbox;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import valueit.observability.platform.dto.AnomalyReport;
import valueit.observability.platform.incident.Severity;

import java.time.Instant;
import java.util.UUID;

@Document(indexName = "anomaly-outbox")
public class AnomalyOutboxEntry {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String type;
    @Field(type = FieldType.Text)
    private String description;
    @Field(type = FieldType.Keyword)
    private Severity severity;
    @Field(type = FieldType.Date)
    private Instant detectedAt;
    @Field(type = FieldType.Keyword)
    private String source;
    @Field(type = FieldType.Keyword)
    private String logId;
    @Field(type = FieldType.Text)
    private String sourceLogMessage;
    @Field(type = FieldType.Date)
    private Instant createdAt;

    protected AnomalyOutboxEntry() {
    }

    public AnomalyOutboxEntry(AnomalyReport report) {
        this.id = UUID.randomUUID().toString();
        this.type = report.type();
        this.description = report.description();
        this.severity = report.severity();
        this.detectedAt = report.detectedAt();
        this.source = report.source();
        this.logId = report.logId();
        this.sourceLogMessage = report.sourceLogMessage();
        this.createdAt = Instant.now();
    }

    public AnomalyReport toReport() {
        return new AnomalyReport(type, description, severity, detectedAt, source, logId, sourceLogMessage);
    }

    public String getId() {
        return id;
    }
}
