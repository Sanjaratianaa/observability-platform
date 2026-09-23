package valueit.observability.platform.parser;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import valueit.observability.platform.model.LogEntry;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(2)
public class ApacheLogParser implements LogParser {

    private static final Pattern APACHE_PATTERN = Pattern.compile(
            "^(\\S+)\\s\\S+\\s\\S+\\s\\[([^]]+)]\\s\"(\\S+)\\s(\\S+)\\s\\S+\"\\s(\\d{3})\\s(\\S+)(?:\\s.*)?$"
    );

    @Override
    public boolean canParse(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return APACHE_PATTERN.matcher(raw.trim()).matches();
    }

    @Override
    public LogEntry parse(String raw) {
        Matcher m = APACHE_PATTERN.matcher(raw.trim());
        if (!m.matches()) {
            throw new LogParserException("Invalid Apache log : " + raw);
        }

        int statusCode = Integer.parseInt(m.group(5));
        String level = statusCode >= 500 ? "ERROR"
                : statusCode >= 400 ? "WARN"
                : "INFO";

        LogEntry entry = new LogEntry();
        entry.setTimestamp(Instant.now());
        entry.setLevel(level);
        entry.setSource("apache");
        entry.setMessage(String.format("%s %s → %s (from %s)",
                m.group(3), m.group(4), m.group(5), m.group(1)));
        return entry;
    }
}
