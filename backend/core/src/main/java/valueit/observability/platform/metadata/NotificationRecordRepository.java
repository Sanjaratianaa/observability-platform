package valueit.observability.platform.metadata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRecordRepository extends JpaRepository<NotificationRecord, Long> {

    List<NotificationRecord> findByIncidentId(String incidentId);

    List<NotificationRecord> findByChannel(String channel);

    List<NotificationRecord> findBySuccessFalse();

    List<NotificationRecord> findTop20ByOrderBySentAtDesc();
}
