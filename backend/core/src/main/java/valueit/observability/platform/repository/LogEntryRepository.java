package valueit.observability.platform.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import valueit.observability.platform.model.LogEntry;

import java.util.List;

public interface LogEntryRepository extends ElasticsearchRepository<LogEntry, String> {
    List<LogEntry> findByLevel(String level);
    List<LogEntry> findBySource(String source);
}
