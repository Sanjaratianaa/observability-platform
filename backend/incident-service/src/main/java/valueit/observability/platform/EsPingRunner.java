package valueit.observability.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import valueit.observability.platform.repository.IncidentRepository;

@Component
public class EsPingRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(EsPingRunner.class);

    private final ElasticsearchOperations elasticsearchOperations;
    private final IncidentRepository incidentRepository;

    public EsPingRunner(ElasticsearchOperations elasticsearchOperations,
                        IncidentRepository incidentRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.incidentRepository = incidentRepository;
    }

    @Override
    public void run(String... args) {
        String clusterName = elasticsearchOperations.cluster().health().getClusterName();
        log.info("Elasticsearch connecté — cluster : {}", clusterName);
        log.info("Index incidents : {} documents", incidentRepository.count());
    }
}
