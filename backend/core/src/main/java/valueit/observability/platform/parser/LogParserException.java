package valueit.observability.platform.parser;

public class LogParserException extends RuntimeException {
    public LogParserException(String message) {
        super(message);
    }

    public LogParserException(String message, Throwable cause) {
        super(message, cause);
    }
}
