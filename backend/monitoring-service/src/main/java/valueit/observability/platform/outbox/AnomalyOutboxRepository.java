package valueit.observability.platform.outbox;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface AnomalyOutboxRepository extends ElasticsearchRepository<AnomalyOutboxEntry, String> {
    List<AnomalyOutboxEntry> findTop100ByOrderByCreatedAtAsc();
}
