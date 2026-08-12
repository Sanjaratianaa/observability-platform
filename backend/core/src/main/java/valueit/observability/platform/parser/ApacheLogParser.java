package valueit.observability.platform.parser;

public class ApacheLogParser implements LogParser {

    private static final Pattern APACHE_PATTERN = Pattern.compile(
            "^(\\S+)\\s\\S+\\s\\S+\\s\\[([^]]+)]\\s\"(\\S+)\\s(\\S+)\\s\\S+\"\\s(\\d{3})\\s(\\S+)$"
    );

    @Override
    public boolean canParse(String raw) {
        if (raw == null || raw.isBlank()) return false;
        return APACHE_PATTERN.matcher(raw.trim()).matches();
    }

    @Override
    public LogEntry parse(String raw) {
        Matcher matcher = APACHE_PATTERN.matcher(raw.trim());
        if (!matcher.matches()) {
            throw new LogParserException("Invalid Apache log format : " + raw);
        }

        String ip = matcher.group(1);
        String method = matcher.group(3);
        String path   = matcher.group(4);
        int statusCode = Integer.parseInt(matcher.group(5));

        String level = statusCode >= 500 ? "ERROR"
                : statusCode >= 400 ? "WARNING"
                : "INFO";

        LogEntry entry = new LogEntry();
        entry.setLevel(level);
        entry.setMessage(method + " " + path + " -> " + statusCode);
        entry.setSource(ip);
        entry.setTimestamp(Instant.now());

        return entry;
    }
}