package valueit.observability.platform.service;

import org.springframework.stereotype.Service;
import valueit.observability.platform.chatops.ChatCommand;

import java.util.Arrays;
import java.util.List;

@Service
public class ChatOpsService {

    private final List<ChatCommand> commands;

    public ChatOpsService(List<ChatCommand> commands) {
        this.commands = commands;
    }

    public String handle(String rawInput) {
        if (rawInput == null || rawInput.isBlank()) {
            return "Commande vide. Tape 'help'.";
        }

        String[] parts = rawInput.split("\\s+");
        String name = parts[0].toLowerCase();

        if(name.equals("help")) {
            return "Commandes disponibles :\n" + commands.stream()
                    .map(ChatCommand::help)
                    .collect(java.util.stream.Collectors.joining("\n"));
        }

        String[] args = Arrays.copyOfRange(parts, 1, parts.length);

        return commands.stream()
                .filter(c -> c.name().equals(name))
                .findFirst()
                .map(c -> c.execute(args))
                .orElse("Commande inconnue : '" + name + "'. Tape 'help'.");
    }
}
