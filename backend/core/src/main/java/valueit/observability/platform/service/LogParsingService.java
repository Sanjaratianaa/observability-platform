package valueit.observability.platform.service;

import org.springframework.stereotype.Service;
import valueit.observability.platform.model.LogEntry;

import java.util.List;

@Service
public class LogParsingService {

    private finale List<LogParser> parsers;

    public LogParsingService(List<LogParser> parsers) {
        this.parsers = parsers;
    }

    public LogEntry parse(String raw) {
        for (LogParser parser : parsers) {
            if (parser.canParse(raw)) {
                return parser.parse(raw);
            }
        }
        throw  new LogParserException("No parser found for log : " + raw);
    }
}