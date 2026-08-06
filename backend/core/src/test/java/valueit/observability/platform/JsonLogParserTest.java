package valueit.observability.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import valueit.observability.platform.model.LogEntry;
import valueit.observability.platform.parser.JsonLogParser;
import valueit.observability.platform.parser.LogParserException;

import static org.junit.jupiter.api.Assertions.*;

class JsonLogParserTest {

    private JsonLogParser parser;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        parser = new JsonLogParser(mapper);
    }

    @Test
    void canParse_returnsTrueForValidJson() {
        assertTrue(parser.canParse("{\"level\":\"INFO\",\"message\":\"hi\"}"));
    }

    @Test
    void canParse_returnsFalseForInvalidInput() {
        assertFalse(parser.canParse(null));
        assertFalse(parser.canParse(""));
        assertFalse(parser.canParse("   "));
        assertFalse(parser.canParse("hello"));
        assertFalse(parser.canParse("{incomplete"));
    }

    @Test
    void parse_validJson_returnsLogEntry() {
        String raw = "{\"timestamp\":\"2026-08-05T14:23:45Z\",\"level\":\"ERROR\",\"source\":\"api\",\"message\":\"boom\"}";
        LogEntry entry = parser.parse(raw);
        assertEquals("ERROR", entry.getLevel());
        assertEquals("api", entry.getSource());
        assertEquals("boom", entry.getMessage());
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void parse_missingTimestamp_setsNow() {
        String raw = "{\"level\":\"INFO\",\"source\":\"app\",\"message\":\"hi\"}";
        LogEntry entry = parser.parse(raw);
        assertNotNull(entry.getTimestamp());
    }

    @Test
    void parse_missingLevel_throws() {
        String raw = "{\"source\":\"app\",\"message\":\"hi\"}";
        assertThrows(LogParserException.class, () -> parser.parse(raw));
    }

    @Test
    void parse_invalidJson_throws() {
        assertThrows(LogParserException.class, () -> parser.parse("{not json"));
    }
}