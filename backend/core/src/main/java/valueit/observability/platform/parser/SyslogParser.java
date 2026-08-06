package valueit.observability.platform.parser;

import org.springframework.stereotype.Component;
import valueit.observability.platform.model.LogEntry;

import java.util.regex.Matcher;

@Component
public class SyslogParser implements LogParser {

    private static final String SYSLOG_PATTERN = "";

    @Override
    public boolean canParse(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return  SYSLOG_PATTERN.matcher(raw.trim()).matches();
    }

    @Override
    public LogEntry parse(String raw) {
        Matcher matcher = SYSLOG_PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            throw new LogParserException("Invalid syslog format : " + raw);
        }

        String pri  = m.group(1);
        String ts   = m.group(2);
        String host = m.group(3);
        String tag  = m.group(4);
        String msg  = m.group(5);

        String level = "INFO";
        if (pri != null) {
            int severity = Integer.parseInt(pri) % 8;
            level = severityToLevel(severity);
        }

    }

    private String severityToLevel (int severity) {
        if (severity > 0 && severity < 3) {
            return "ERROR";
        } else if (severity == 4) {
            return "WARNING";
        } else if (severity > 5 && severity < 6) {
            return "INFO";
        } else if (severity == 7) {
            return "DEBUG";
        }

        return null;
    }
}
