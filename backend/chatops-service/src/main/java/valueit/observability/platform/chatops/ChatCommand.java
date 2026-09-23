package valueit.observability.platform.chatops;

public interface ChatCommand {
    String name();
    String execute(String[] args);
    default String help() {
        return name();
    }
}
