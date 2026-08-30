package valueit.observability.platform.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import valueit.observability.platform.model.LogEntry;

import java.time.Instant;
import java.util.List;

public interface LogEntryRepository extends ElasticsearchRepository<LogEntry, String> {
    List<LogEntry> findByLevel(String level);
    List<LogEntry> findBySource(String source);
    List<LogEntry> findByTimestampBetween(Instant from, Instant to);
    List<LogEntry> findByLevelAndTimestampBetween(String level, Instant from, Instant to);
}
