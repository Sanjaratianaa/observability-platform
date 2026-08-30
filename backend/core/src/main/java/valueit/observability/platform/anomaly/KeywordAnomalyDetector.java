package valueit.observability.platform.anomaly;

import org.springframework.stereotype.Component;
import valueit.observability.platform.incident.Severity;
import valueit.observability.platform.model.LogEntry;

import java.util.List;
import java.util.Optional;

@Component
public class KeywordAnomalyDetector implements AnomalyDetector {

    private record KeywordRule(String keyword, Severity severity, String description) {}

    private static final List<KeywordRule> RULES = List.of(
            new KeywordRule("OutOfMemoryError", Severity.CRITICAL, "Mémoire épuisée (OOM)"),
            new KeywordRule("StackOverflowError", Severity.CRITICAL, "Débordement de pile"),
            new KeywordRule("deadlock", Severity.CRITICAL, "Deadlock détecté"),
            new KeywordRule("fatal", Severity.HIGH, "Erreur fatale"),
            new KeywordRule("connection refused", Severity.HIGH, "Connexion refusée"),
            new KeywordRule("disk full", Severity.HIGH, "Disque plein"),
            new KeywordRule("permission denied", Severity.MEDIUM, "Permission refusée"),
            new KeywordRule("timeout", Severity.MEDIUM, "Timeout détecté")
    );

    @Override
    public Optional<Anomaly> detect(LogEntry entry) {
        if (entry.getMessage() == null) {
            return Optional.empty();
        }

        String messageLower = entry.getMessage().toLowerCase();

        for (KeywordRule rule : RULES) {
            if (messageLower.contains(rule.keyword().toLowerCase())) {
                Anomaly anomaly = new Anomaly(
                        "KEYWORD_" + rule.keyword().toUpperCase().replace(" ", "_"),
                        rule.description() + " dans le message : « " + truncate(entry.getMessage(), 100) + " »",
                        rule.severity(),
                        entry.getMessage()
                );
                return Optional.of(anomaly);
            }
        }

        return Optional.empty();
    }

    private String truncate(String text, int maxLength) {
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
