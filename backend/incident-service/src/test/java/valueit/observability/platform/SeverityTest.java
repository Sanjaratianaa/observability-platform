package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import valueit.observability.platform.incident.Severity;

import static org.junit.jupiter.api.Assertions.*;

class SeverityTest {

    @Test
    void max_returnsHigherSeverity() {
        assertEquals(Severity.HIGH, Severity.LOW.max(Severity.HIGH));
        assertEquals(Severity.HIGH, Severity.HIGH.max(Severity.LOW));
    }

    @Test
    void max_sameValues_returnsSelf() {
        assertEquals(Severity.MEDIUM, Severity.MEDIUM.max(Severity.MEDIUM));
    }

    @Test
    void max_critical_alwaysWins() {
        assertEquals(Severity.CRITICAL, Severity.LOW.max(Severity.CRITICAL));
        assertEquals(Severity.CRITICAL, Severity.CRITICAL.max(Severity.LOW));
        assertEquals(Severity.CRITICAL, Severity.CRITICAL.max(Severity.CRITICAL));
    }

    @Test
    void ordinalOrder_isCorrect() {
        assertTrue(Severity.LOW.ordinal() < Severity.MEDIUM.ordinal());
        assertTrue(Severity.MEDIUM.ordinal() < Severity.HIGH.ordinal());
        assertTrue(Severity.HIGH.ordinal() < Severity.CRITICAL.ordinal());
    }
}
