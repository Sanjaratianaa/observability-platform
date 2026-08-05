package valueit.observability.platform;

import org.springframework.boot.CommandLineRunner;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.repository.LogEntryRepository;

@Component
public class ElasticSearchPingRunner implements CommandLineRunner {

    private final ElasticsearchOperations elasticsearchOperations;
    private final LogEntryRepository logEntryRepository;

    public ElasticSearchPingRunner (ElasticsearchOperations elasticsearchOperations,
                                    LogEntryRepository logEntryRepository) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.logEntryRepository = logEntryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Vérification de la connexion
        String clusterName = elasticsearchOperations.cluster().health().getClusterName();
        System.out.println("Elasticsearch is connected. Cluster : " + clusterName);

        // Insertion de log fictif
        LogEntry log = new LogEntry("ERROR", "api-gateway", "Connexion timeout to downstream service");
        LogEntry saved = logEntryRepository.save(log);
        System.out.println("Log saved with ID : " + saved.getId());

        // relire par ID
        logEntryRepository.findById(saved.getId()).ifPresent(found ->
                System.out.println("Log retrieved : " + found)
        );

        // Compter les documents dans l'index
        long count = logEntryRepository.count();
        System.out.println("Number of logs in index : " + count);
    }
}