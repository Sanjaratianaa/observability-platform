package valueit.observability.platform;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI incidentOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Incident Service API")
                        .description("Gestion des incidents : corrélation, déduplication, "
                                + "notifications Jira/Teams et journal d'audit.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MBDS M2 — Stage ValueIT")
                                .email("sanjaratiana@mbds.mg"))
                        .license(new License()
                                .name("MIT")));
    }
}
