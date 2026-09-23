package valueit.observability.platform;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import valueit.observability.platform.anomaly.Anomaly;
import valueit.observability.platform.incident.Incident;
import valueit.observability.platform.incident.IncidentEvent;
import valueit.observability.platform.incident.IncidentStatus;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.metadata.AuditService;
import valueit.observability.platform.metrics.PlatformMetrics;
import valueit.observability.platform.notification.NotificationHub;
import valueit.observability.platform.repository.IncidentRepository;
import valueit.observability.platform.service.IncidentService;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private NotificationHub notificationHub;

    @Mock
    private AuditService auditService;

    @Mock
    private PlatformMetrics metrics;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository, notificationHub, auditService, metrics);
    }

    private LogEntry makeLog(String source, String message) {
        LogEntry e = new LogEntry();
        e.setId("log-123");
        e.setSource(source);
        e.setLevel("ERROR");
        e.setMessage(message);
        e.setTimestamp(Instant.now());
        return e;
    }

    private Anomaly makeAnomaly() {
        return new Anomaly("STACK_TRACE_EXCEPTION", "NPE detected", Severity.HIGH, "stack trace...");
    }

    @Test
    void handle_newAnomaly_createsIncident() {
        when(incidentRepository.findFirstByFingerprintAndStatusIn(any(), any()))
                .thenReturn(Optional.empty());
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Incident result = incidentService.handle(makeAnomaly(), makeLog("api-gw", "boom"));

        assertEquals("STACK_TRACE_EXCEPTION", result.getType());
        assertEquals(Severity.HIGH, result.getSeverity());
        assertEquals(IncidentStatus.OPEN, result.getStatus());
        assertEquals("api-gw", result.getSource());
        assertEquals(1, result.getOccurrenceCount());
        assertTrue(result.getRelatedLogIds().contains("log-123"));
        verify(notificationHub).dispatch(any(Incident.class), eq(IncidentEvent.CREATED));
    }

    @Test
    void handle_existingIncident_updatesRecurrence() {
        Incident existing = new Incident();
        existing.setFingerprint("STACK_TRACE_EXCEPTION::api-gw");
        existing.setStatus(IncidentStatus.OPEN);
        existing.setSeverity(Severity.MEDIUM);
        existing.setOccurrenceCount(3);
        existing.setLastSeen(Instant.now().minusSeconds(60));

        when(incidentRepository.findFirstByFingerprintAndStatusIn(any(), any()))
                .thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Incident result = incidentService.handle(makeAnomaly(), makeLog("api-gw", "boom"));

        assertEquals(4, result.getOccurrenceCount());
        assertEquals(Severity.HIGH, result.getSeverity()); // escalated from MEDIUM to HIGH
        assertTrue(result.getRelatedLogIds().contains("log-123"));
        verify(notificationHub).dispatch(any(Incident.class), eq(IncidentEvent.RECURRED));
    }

    @Test
    void handle_acknowledgedIncident_updatesRecurrenceWithoutReopening() {
        Incident existing = new Incident();
        existing.setFingerprint("STACK_TRACE_EXCEPTION::api-gw");
        existing.setStatus(IncidentStatus.ACKNOWLEDGED);
        existing.setSeverity(Severity.MEDIUM);
        existing.setOccurrenceCount(2);
        existing.setLastSeen(Instant.now().minusSeconds(60));

        when(incidentRepository.findFirstByFingerprintAndStatusIn(any(), any()))
                .thenReturn(Optional.of(existing));
        when(incidentRepository.save(any(Incident.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Incident result = incidentService.handle(makeAnomaly(), makeLog("api-gw", "boom"));

        assertEquals(3, result.getOccurrenceCount());
        assertEquals(IncidentStatus.ACKNOWLEDGED, result.getStatus()); // pas de doublon, statut conservé
        verify(notificationHub).dispatch(any(Incident.class), eq(IncidentEvent.RECURRED));
    }

    @Test
    void acknowledge_existingIncident_setsAcknowledged() {
        Incident inc = new Incident();
        inc.setStatus(IncidentStatus.OPEN);
        when(incidentRepository.findById("id-1")).thenReturn(Optional.of(inc));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Incident> result = incidentService.acknowledge("id-1");

        assertTrue(result.isPresent());
        assertEquals(IncidentStatus.ACKNOWLEDGED, result.get().getStatus());
    }

    @Test
    void acknowledge_nonExistent_returnsEmpty() {
        when(incidentRepository.findById("nope")).thenReturn(Optional.empty());
        assertTrue(incidentService.acknowledge("nope").isEmpty());
    }

    @Test
    void acknowledge_nonOpenIncident_throws() {
        Incident inc = new Incident();
        inc.setStatus(IncidentStatus.ACKNOWLEDGED);
        when(incidentRepository.findById("id-ack")).thenReturn(Optional.of(inc));

        assertThrows(IllegalArgumentException.class, () -> incidentService.acknowledge("id-ack"));
        verify(incidentRepository, never()).save(any());
    }

    @Test
    void resolve_existingIncident_setsResolved() {
        Incident inc = new Incident();
        inc.setStatus(IncidentStatus.ACKNOWLEDGED);
        when(incidentRepository.findById("id-2")).thenReturn(Optional.of(inc));
        when(incidentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Optional<Incident> result = incidentService.resolve("id-2");

        assertTrue(result.isPresent());
        assertEquals(IncidentStatus.RESOLVED, result.get().getStatus());
        verify(notificationHub).dispatch(any(Incident.class), eq(IncidentEvent.RESOLVED));
    }

    @Test
    void resolve_nonExistent_returnsEmpty() {
        when(incidentRepository.findById("nope")).thenReturn(Optional.empty());
        assertTrue(incidentService.resolve("nope").isEmpty());
    }

    @Test
    void resolve_alreadyResolved_throws() {
        Incident inc = new Incident();
        inc.setStatus(IncidentStatus.RESOLVED);
        when(incidentRepository.findById("id-res")).thenReturn(Optional.of(inc));

        assertThrows(IllegalArgumentException.class, () -> incidentService.resolve("id-res"));
        verify(incidentRepository, never()).save(any());
    }
}
