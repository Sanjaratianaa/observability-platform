package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.parser.SyslogParser;
import valueit.observability.platform.parser.LogParserException;

import static org.junit.jupiter.api.Assertions.*;

class SyslogParserTest {

    private final SyslogParser parser = new SyslogParser();

    @Test
    void canParse_validSyslog_returnsTrue() {
        String raw = "<34>Oct  5 12:34:56 myhost sshd: Failed password for root";
        assertTrue(parser.canParse(raw));
    }

    @Test
    void canParse_invalidInput_returnsFalse() {
        assertFalse(parser.canParse(null));
        assertFalse(parser.canParse(""));
        assertFalse(parser.canParse("{\"json\": true}"));
        assertFalse(parser.canParse("random text"));
    }

    @Test
    void parse_criticalSeverity_returnsError() {
        // priority 2 → severity = 2 % 8 = 2 → ERROR
        String raw = "<2>Oct  5 12:34:56 server kernel: Kernel panic";
        LogEntry entry = parser.parse(raw);
        assertEquals("ERROR", entry.getLevel());
        assertEquals("Kernel panic", entry.getMessage());
        assertEquals("kernel", entry.getSource());
    }

    @Test
    void parse_warningSeverity_returnsWarning() {
        // priority 12 → severity = 12 % 8 = 4 → WARN
        String raw = "<12>Oct  5 12:34:56 server app: Low disk space";
        LogEntry entry = parser.parse(raw);
        assertEquals("WARN", entry.getLevel());
    }

    @Test
    void parse_infoSeverity_returnsInfo() {
        // priority 14 → severity = 14 % 8 = 6 → INFO
        String raw = "<14>Oct  5 12:34:56 server app: Service started";
        LogEntry entry = parser.parse(raw);
        assertEquals("INFO", entry.getLevel());
    }

    @Test
    void parse_debugSeverity_returnsDebug() {
        // priority 15 → severity = 15 % 8 = 7 → DEBUG
        String raw = "<15>Oct  5 12:34:56 server app: Debug trace";
        LogEntry entry = parser.parse(raw);
        assertEquals("DEBUG", entry.getLevel());
    }

    @Test
    void parse_invalidFormat_throws() {
        assertThrows(LogParserException.class, () -> parser.parse("not syslog"));
    }
}
