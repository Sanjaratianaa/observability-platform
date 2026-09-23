package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.parser.ApacheLogParser;
import valueit.observability.platform.parser.LogParserException;

import static org.junit.jupiter.api.Assertions.*;

class ApacheLogParserTest {

    private final ApacheLogParser parser = new ApacheLogParser();

    @Test
    void canParse_validApacheLog_returnsTrue() {
        String raw = "192.168.1.1 - - [10/Oct/2026:13:55:36 +0000] \"GET /index.html HTTP/1.1\" 200 2326";
        assertTrue(parser.canParse(raw));
    }

    @Test
    void canParse_invalidInput_returnsFalse() {
        assertFalse(parser.canParse(null));
        assertFalse(parser.canParse(""));
        assertFalse(parser.canParse("random text"));
        assertFalse(parser.canParse("{\"json\": true}"));
    }

    @Test
    void parse_status200_returnsInfo() {
        String raw = "10.0.0.1 - - [10/Oct/2026:13:55:36 +0000] \"GET /api/health HTTP/1.1\" 200 512";
        LogEntry entry = parser.parse(raw);
        assertEquals("INFO", entry.getLevel());
        assertEquals("GET /api/health -> 200", entry.getMessage());
        assertEquals("10.0.0.1", entry.getSource());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void parse_status500_returnsError() {
        String raw = "10.0.0.1 - - [10/Oct/2026:13:55:36 +0000] \"POST /api/data HTTP/1.1\" 500 0";
        LogEntry entry = parser.parse(raw);
        assertEquals("ERROR", entry.getLevel());
    }

    @Test
    void parse_status404_returnsWarning() {
        String raw = "10.0.0.1 - - [10/Oct/2026:13:55:36 +0000] \"GET /missing HTTP/1.1\" 404 0";
        LogEntry entry = parser.parse(raw);
        assertEquals("WARN", entry.getLevel());
    }

    @Test
    void parse_invalidFormat_throws() {
        assertThrows(LogParserException.class, () -> parser.parse("not apache"));
    }
}
