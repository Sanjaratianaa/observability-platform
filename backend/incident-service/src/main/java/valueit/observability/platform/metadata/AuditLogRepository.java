package valueit.observability.platform.metadata;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);

    List<AuditLog> findByAction(String action);

    List<AuditLog> findByCreatedAtBetween(Instant from, Instant to);

    List<AuditLog> findTop20ByOrderByCreatedAtDesc();
}
