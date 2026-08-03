package valueit.observability.platform;

public class ElasticSearchPingRunner implements CommandLineRunner {

    private final ElasticsearchTemplate elasticsearchTemplate;

    public ElasticSearchPingRunner (ElasticsearchTemplate elasticsearchTemplate) {
        this.elasticsearchTemplate = elasticsearchTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean isUp = elasticsearchTemplate.getClusterHealth() != null;
        System.out.println("Elasticsearch is " + (isUp ? "up" : "down"));
    }
}