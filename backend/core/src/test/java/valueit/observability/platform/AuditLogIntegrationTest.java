package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import valueit.observability.platform.metadata.AuditLog;
import valueit.observability.platform.metadata.AuditLogRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "INTEGRATION_TESTS", matches = "true")
class AuditLogIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void save_and_retrieve_auditLog() {
        AuditLog log = new AuditLog();
        log.setAction("INCIDENT_CREATED");
        log.setEntityType("INCIDENT");
        log.setEntityId("inc-001");
        log.setDetails("NPE detected");
        log.setPerformedBy("system");

        AuditLog saved = auditLogRepository.save(log);
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void findByEntityTypeAndEntityId_returnsResults() {
        AuditLog log1 = new AuditLog();
        log1.setAction("INCIDENT_CREATED");
        log1.setEntityType("INCIDENT");
        log1.setEntityId("inc-002");
        log1.setPerformedBy("system");
        auditLogRepository.save(log1);

        AuditLog log2 = new AuditLog();
        log2.setAction("INCIDENT_ACKNOWLEDGED");
        log2.setEntityType("INCIDENT");
        log2.setEntityId("inc-002");
        log2.setPerformedBy("user");
        auditLogRepository.save(log2);

        List<AuditLog> results = auditLogRepository.findByEntityTypeAndEntityId("INCIDENT", "inc-002");
        assertEquals(2, results.size());
    }

    @Test
    void findTop20ByOrderByCreatedAtDesc_returnsLatest() {
        for (int i = 0; i < 25; i++) {
            AuditLog log = new AuditLog();
            log.setAction("ACTION_" + i);
            log.setEntityType("TEST");
            log.setEntityId("id-" + i);
            log.setPerformedBy("system");
            auditLogRepository.save(log);
        }
        List<AuditLog> latest = auditLogRepository.findTop20ByOrderByCreatedAtDesc();
        assertEquals(20, latest.size());
    }
}
