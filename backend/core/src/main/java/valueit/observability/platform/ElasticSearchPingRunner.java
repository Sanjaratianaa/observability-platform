package valueit.observability.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import valueit.observability.platform.repository.LogEntryRepository;
import valueit.observability.platform.repository.IncidentRepository;

@Component
public class ElasticSearchPingRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ElasticSearchPingRunner.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final LogEntryRepository logEntryRepository;
    private final IncidentRepository incidentRepository;

    public ElasticSearchPingRunner(ElasticsearchOperations elasticsearchOperations,
                                   LogEntryRepository logEntryRepository,
                                   IncidentRepository incidentRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.logEntryRepository = logEntryRepository;
        this.incidentRepository = incidentRepository;
    }

    @Override
    public void run(String... args) {
        String clusterName = elasticsearchOperations.cluster().health().getClusterName();
        log.info("Elasticsearch connecté — cluster : {}", clusterName);
        log.info("Index logs : {} documents", logEntryRepository.count());
        log.info("Index incidents : {} documents", incidentRepository.count());
    }
}