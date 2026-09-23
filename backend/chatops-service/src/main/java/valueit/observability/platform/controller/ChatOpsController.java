package valueit.observability.platform.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import valueit.observability.platform.service.ChatOpsService;

@Tag(name = "ChatOps", description = "Interface conversationnelle — commandes : list, stats, ack <id>, resolve <id>")
@RestController
@RequestMapping("/api/chatops")
public class ChatOpsController {

    private final ChatOpsService chatOpsService;

    public ChatOpsController(ChatOpsService chatOpsService) {
        this.chatOpsService = chatOpsService;
    }

    @Operation(summary = "Exécuter une commande ChatOps", description = "Envoyer une commande texte (ex: 'list', 'stats', 'ack <id>') et recevoir la réponse")
    @PostMapping
    public String command(@RequestBody String rawInput) {
        return chatOpsService.handle(rawInput);
    }
}
