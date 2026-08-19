package valueit.observability.platform.incident;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

@Document(indexName = "incidents")
public class Incident {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String fingerprint;

    @Field(type = FieldType.Keyword)
    private String type;

    @Field(type = FieldType.Keyword)
    private String severity;

    @Field(type = FieldType.Keyword)
    private IncidentStatus status;

    @Field(type = FieldType.Keyword)
    private String source;

    @Field(type = FieldType.Text)
    private String description;

    @Field(type = FieldType.Date)
    private Instant firstSeen;

    @Field(type = FieldType.Date)
    private Instant lastSeen;

    @Field(type = FieldType.Integer)
    private int occurrenceCount;

    @Field(type = FieldType.Keyword)
    private String jiraTicketKey;

    public Incident() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public IncidentStatus getStatus() { return status; }
    public void setStatus(IncidentStatus status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Instant firstSeen) { this.firstSeen = firstSeen; }

    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }

    public int getOccurrenceCount() { return occurrenceCount; }
    public void setOccurrenceCount(int occurrenceCount) { this.occurrenceCount = occurrenceCount; }

    public String getJiraTicketKey() { return jiraTicketKey; }
    public void setJiraTicketKey(String jiraTicketKey) { this.jiraTicketKey = jiraTicketKey; }
}
