package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import valueit.observability.platform.metadata.NotificationRecord;
import valueit.observability.platform.metadata.NotificationRecordRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "INTEGRATION_TESTS", matches = "true")
class NotificationRecordIntegrationTest {

    @Autowired
    private NotificationRecordRepository notificationRecordRepository;

    @Test
    void save_and_retrieve_notificationRecord() {
        NotificationRecord record = new NotificationRecord();
        record.setChannel("TEAMS");
        record.setIncidentId("inc-001");
        record.setEventType("CREATED");
        record.setSuccess(true);
        record.setExternalRef("msg-123");

        NotificationRecord saved = notificationRecordRepository.save(record);
        assertNotNull(saved.getId());
        assertNotNull(saved.getSentAt());
    }

    @Test
    void findByIncidentId_returnsResults() {
        NotificationRecord r1 = new NotificationRecord();
        r1.setChannel("TEAMS");
        r1.setIncidentId("inc-003");
        r1.setEventType("CREATED");
        r1.setSuccess(true);
        notificationRecordRepository.save(r1);

        NotificationRecord r2 = new NotificationRecord();
        r2.setChannel("JIRA");
        r2.setIncidentId("inc-003");
        r2.setEventType("CREATED");
        r2.setSuccess(false);
        r2.setErrorMessage("Connection refused");
        notificationRecordRepository.save(r2);

        assertEquals(2, notificationRecordRepository.findByIncidentId("inc-003").size());
    }

    @Test
    void findBySuccessFalse_returnsFailures() {
        NotificationRecord ok = new NotificationRecord();
        ok.setChannel("TEAMS");
        ok.setIncidentId("inc-004");
        ok.setEventType("CREATED");
        ok.setSuccess(true);
        notificationRecordRepository.save(ok);

        NotificationRecord fail = new NotificationRecord();
        fail.setChannel("JIRA");
        fail.setIncidentId("inc-005");
        fail.setEventType("CREATED");
        fail.setSuccess(false);
        fail.setErrorMessage("Timeout");
        notificationRecordRepository.save(fail);

        List<NotificationRecord> failures = notificationRecordRepository.findBySuccessFalse();
        assertEquals(1, failures.size());
        assertEquals("JIRA", failures.get(0).getChannel());
    }
}
