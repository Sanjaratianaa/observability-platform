package valueit.observability.platform.parser;

import org.springframework.stereotype.Component;
import valueit.observability.platform.model.LogEntry;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.time.Instant;

@Component
public class JsonLogParser implements LogParser{
    private final ObjectMapper objectMapper;

    public JsonLogParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean canParse(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String trimmed = raw.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }

    @Override
    public LogEntry parse(String raw) {
        try {
            LogEntry entry = objectMapper.readValue(raw, LogEntry.class);
            if (entry.getLevel() == null || entry.getMessage() == null) {
                throw new LogParserException("Missing required field : level or message ");
            }


            if (entry.getTimestamp() == null) {
                entry.setTimestamp(Instant.now());
            }

            return entry;
        } catch (JacksonException e) {
            throw new LogParserException("Invalid JSON log : " + raw, e);
        }
    }
}
