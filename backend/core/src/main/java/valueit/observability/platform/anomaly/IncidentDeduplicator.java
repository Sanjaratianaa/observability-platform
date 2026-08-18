package valueit.observability.platform.anomaly;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IncidentDeduplicator {
    private static final long COOLDOWN_SECONDS = 900;

    private final ConcurrentHashMap<String, Instant> recentFingerprints = new ConcurrentHashMap<>();

    public boolean isNew(Anomaly anomaly) {
        String fingerprint = buildFingerprint(anomaly);
        Instant now = Instant.now();

        Instant lastSeen = recentFingerprints.get(fingerprint);
        if (lastSeen != null && lastSeen.plusSeconds(COOLDOWN_SECONDS).isAfter(now)) {
            return false;
        }

        recentFingerprints.put(fingerprint, now);
        evictOldEntries(now);
        return true;
    }

    private String buildFingerprint(Anomaly anomaly) {
        return anomaly.getType() + "::" + anomaly.getDescription().hashCode();
    }

    private void evictOldEntries(Instant now) {
        recentFingerprints.entrySet().removeIf(
            entry -> entry.getValue().plusSeconds(COOLDOWN_SECONDS * 2).isBefore(now)
        );
    }
}
