package valueit.observability.platform.incident;

public enum Severity {
    LOW, MEDIUM, HIGH, CRITICAL;

    public Severity max(Severity other) {
        return this.ordinal() >= other.ordinal() ? this : other;
    }
}
