package valueit.observability.platform.parser;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import valueit.observability.platform.model.LogEntry;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(3)
public class SyslogParser implements LogParser {

    private static final Pattern SYSLOG_PATTERN = Pattern.compile(
            "^(?:<(\\d+)>)?(\\w{3}\\s+\\d{1,2}\\s\\d{2}:\\d{2}:\\d{2})\\s(\\S+)\\s(\\S+):\\s(.*)$"
    );

    // Niveau inféré depuis le début du message quand le PRI syslog est absent
    private static final Pattern LEVEL_PREFIX = Pattern.compile(
            "^(?i)(emerg|alert|crit|critical|error|err|warn|warning|notice|info|debug)\\b"
    );

    @Override
    public boolean canParse(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return SYSLOG_PATTERN.matcher(raw.trim()).matches();
    }

    @Override
    public LogEntry parse(String raw) {
        Matcher m = SYSLOG_PATTERN.matcher(raw.trim());
        if (!m.matches()) {
            throw new LogParserException("Invalid syslog line : " + raw);
        }

        String pri = m.group(1);
        String host = m.group(3);
        String app = m.group(4);
        String msg = m.group(5);

        String level = pri != null
                ? levelFromPri(Integer.parseInt(pri))
                : inferLevelFromMessage(msg);

        LogEntry entry = new LogEntry();
        entry.setTimestamp(Instant.now());
        entry.setLevel(level);
        entry.setSource(app != null ? app : host);
        entry.setMessage(msg);
        return entry;
    }

    private String inferLevelFromMessage(String msg) {
        Matcher m = LEVEL_PREFIX.matcher(msg);
        if (!m.find()) {
            return "INFO";
        }
        return switch (m.group(1).toLowerCase()) {
            case "emerg", "alert", "crit", "critical", "error", "err" -> "ERROR";
            case "warn", "warning" -> "WARN";
            case "debug" -> "DEBUG";
            default -> "INFO"; // info, notice
        };
    }

    private String levelFromPri(int pri) {
        int severity = pri % 8;
        return switch (severity) {
            case 0, 1, 2, 3 -> "ERROR";
            case 4 -> "WARN";
            case 5, 6 -> "INFO";
            default -> "DEBUG";
        };
    }
}
