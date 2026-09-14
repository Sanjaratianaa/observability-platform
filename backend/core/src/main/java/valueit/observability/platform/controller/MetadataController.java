package valueit.observability.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import valueit.observability.platform.metadata.AuditLog;
import valueit.observability.platform.metadata.AuditLogRepository;
import valueit.observability.platform.metadata.NotificationRecord;
import valueit.observability.platform.metadata.NotificationRecordRepository;

import java.util.List;

@Tag(name = "Metadata", description = "Audit trail et historique des notifications (PostgreSQL)")
@RestController
@RequestMapping("/api/metadata")
public class MetadataController {

    private final AuditLogRepository auditLogRepository;
    private final NotificationRecordRepository notificationRecordRepository;

    public MetadataController(AuditLogRepository auditLogRepository,
                              NotificationRecordRepository notificationRecordRepository) {
        this.auditLogRepository = auditLogRepository;
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @Operation(summary = "Dernières actions auditées")
    @GetMapping("/audit")
    public List<AuditLog> recentAudit() {
        return auditLogRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @Operation(summary = "Historique d'audit d'un incident")
    @GetMapping("/audit/incident/{incidentId}")
    public List<AuditLog> auditForIncident(@PathVariable String incidentId) {
        return auditLogRepository.findByEntityTypeAndEntityId("INCIDENT", incidentId);
    }

    @Operation(summary = "Dernières notifications envoyées")
    @GetMapping("/notifications")
    public List<NotificationRecord> recentNotifications() {
        return notificationRecordRepository.findTop20ByOrderBySentAtDesc();
    }

    @Operation(summary = "Notifications en échec")
    @GetMapping("/notifications/failed")
    public List<NotificationRecord> failedNotifications() {
        return notificationRecordRepository.findBySuccessFalse();
    }
}
