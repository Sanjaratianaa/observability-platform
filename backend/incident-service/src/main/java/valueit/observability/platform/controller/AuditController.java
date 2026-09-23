package valueit.observability.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import valueit.observability.platform.metadata.AuditLog;
import valueit.observability.platform.metadata.AuditLogRepository;
import valueit.observability.platform.metadata.NotificationRecord;
import valueit.observability.platform.metadata.NotificationRecordRepository;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Audit", description = "Journal d'audit et historique des notifications")
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final NotificationRecordRepository notificationRecordRepository;

    public AuditController(AuditLogRepository auditLogRepository,
                           NotificationRecordRepository notificationRecordRepository) {
        this.auditLogRepository = auditLogRepository;
        this.notificationRecordRepository = notificationRecordRepository;
    }

    @GetMapping("/audit")
    @Operation(summary = "Dernières entrées du journal d'audit")
    public List<AuditLog> recentAudit(@RequestParam(required = false) String action) {
        if (action != null && !action.isBlank()) {
            return auditLogRepository.findByAction(action);
        }
        return auditLogRepository.findTop20ByOrderByCreatedAtDesc();
    }

    @GetMapping("/audit/incident/{incidentId}")
    @Operation(summary = "Historique d'audit d'un incident")
    public List<AuditLog> auditForIncident(@PathVariable String incidentId) {
        return auditLogRepository.findByEntityTypeAndEntityId("INCIDENT", incidentId);
    }

    @GetMapping("/notifications")
    @Operation(summary = "Derniers enregistrements de notifications envoyées")
    public List<NotificationRecord> recentNotifications(@RequestParam(required = false) String channel) {
        if (channel != null && !channel.isBlank()) {
            return notificationRecordRepository.findByChannel(channel);
        }
        return notificationRecordRepository.findTop20ByOrderBySentAtDesc();
    }

    @GetMapping("/notifications/failed")
    @Operation(summary = "Notifications en échec")
    public List<NotificationRecord> failedNotifications() {
        return notificationRecordRepository.findBySuccessFalse();
    }
}
