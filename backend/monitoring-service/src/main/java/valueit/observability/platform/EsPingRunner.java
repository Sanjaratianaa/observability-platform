package valueit.observability.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import valueit.observability.platform.repository.LogEntryRepository;

@Component
public class EsPingRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EsPingRunner.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final LogEntryRepository logEntryRepository;

    public EsPingRunner(ElasticsearchOperations elasticsearchOperations,
                        LogEntryRepository logEntryRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.logEntryRepository = logEntryRepository;
    }

    @Override
    public void run(String... args) {
        String clusterName = elasticsearchOperations.cluster().health().getClusterName();
        log.info("Elasticsearch connecté — cluster : {}", clusterName);
        log.info("Index logs : {} documents", logEntryRepository.count());
    }
}
