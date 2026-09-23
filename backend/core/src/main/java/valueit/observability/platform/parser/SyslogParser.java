package valueit.observability.platform.parser;

import org.springframework.stereotype.Component;
import valueit.observability.platform.model.LogEntry;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
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
        Matcher matcher = SYSLOG_PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            throw new LogParserException("Invalid syslog format : " + raw);
        }

        String pri  = matcher.group(1);
        String host = matcher.group(3);
        String tag  = matcher.group(4);
        String msg  = matcher.group(5);

        String level;
        if (pri != null) {
            int severity = Integer.parseInt(pri) % 8;
            level = severityToLevel(severity);
        } else {
            level = inferLevelFromMessage(msg);
        }

        LogEntry entry = new LogEntry();
        entry.setLevel(level);
        entry.setMessage(msg);
        entry.setSource(host + "/" + tag);
        entry.setTimestamp(Instant.now());

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

    private String severityToLevel(int severity) {
        return switch (severity) {
            case 0, 1, 2, 3 -> "ERROR";
            case 4 -> "WARN";
            case 5, 6 -> "INFO";
            case 7 -> "DEBUG";
            default -> "INFO";
        };
    }
}