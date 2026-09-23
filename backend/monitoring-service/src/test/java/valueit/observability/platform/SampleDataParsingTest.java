package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import valueit.observability.platform.parser.ApacheLogParser;
import valueit.observability.platform.parser.JsonLogParser;
import valueit.observability.platform.parser.LogParser;
import valueit.observability.platform.service.LogParsingService;
import valueit.observability.platform.model.LogEntry;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SampleDataParsingTest {

    @Test
    void parseSampleFiles_findErrorEntries() throws Exception {
        // Initialize parsers (as in application context)
        ObjectMapper mapper = new ObjectMapper();
        List<LogParser> parsers = new ArrayList<>();
        parsers.add(new ApacheLogParser());
        parsers.add(new JsonLogParser(mapper));
        // Use service to pick appropriate parser
        LogParsingService parsingService = new LogParsingService(parsers);

        // Paths: from backend/monitoring-service module to repo root it's ../.. then data/sample
        Path repoSample = Path.of("..", "..", "data", "sample");

        // Apache log
        Path apache = repoSample.resolve("apache.log");
        long apacheErrors = Files.lines(apache)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("#"))
                .map(line -> {
                    try {
                        LogEntry e = parsingService.parse(line);
                        return e.getLevel();
                    } catch (Exception ex) {
                        return null;
                    }
                })
                .filter(level -> level != null && level.equals("ERROR"))
                .count();

        // JSON log
        Path json = repoSample.resolve("json.log");
        long jsonErrors = Files.lines(json)
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("#"))
                .map(line -> {
                    try {
                        LogEntry e = parsingService.parse(line);
                        return e.getLevel();
                    } catch (Exception ex) {
                        return null;
                    }
                })
                .filter(level -> level != null && level.equals("ERROR"))
                .count();

        assertTrue(apacheErrors > 0, "Expected ERROR entries in apache.log");
        assertTrue(jsonErrors > 0, "Expected ERROR entries in json.log");
    }
}
