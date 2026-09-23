package valueit.observability.platform;

import org.junit.jupiter.api.Test;
import valueit.observability.platform.chatops.ChatCommand;
import valueit.observability.platform.service.ChatOpsService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatOpsServiceTest {

    private ChatCommand fakeCommand() {
        return new ChatCommand() {
            @Override
            public String name() {
                return "ping";
            }

            @Override
            public String help() {
                return "ping — répond pong";
            }

            @Override
            public String execute(String[] args) {
                return "pong";
            }
        };
    }

    @Test
    void handle_knownCommand_executes() {
        ChatOpsService service = new ChatOpsService(List.of(fakeCommand()));
        assertEquals("pong", service.handle("ping"));
    }

    @Test
    void handle_unknownCommand_returnsError() {
        ChatOpsService service = new ChatOpsService(List.of(fakeCommand()));
        assertTrue(service.handle("nope").contains("Commande inconnue"));
    }

    @Test
    void handle_emptyInput_returnsHelpHint() {
        ChatOpsService service = new ChatOpsService(List.of(fakeCommand()));
        assertTrue(service.handle("").contains("help"));
        assertTrue(service.handle("   ").contains("help"));
    }

    @Test
    void handle_help_listsCommands() {
        ChatOpsService service = new ChatOpsService(List.of(fakeCommand()));
        String out = service.handle("help");
        assertTrue(out.contains("ping — répond pong"));
    }
}
