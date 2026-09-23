package valueit.observability.platform.parser;

import valueit.observability.platform.model.LogEntry;

public interface LogParser {
    boolean canParse(String raw);
    LogEntry parse(String raw);
    default String getName() {
        return getClass().getSimpleName();
    }
}
