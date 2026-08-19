package valueit.observability.platform.controller;

import org.springframework.web.bind.annotation.*;
import valueit.observability.platform.service.ChatOpsService;

@RestController
@RequestMapping("/api/chatops")
public class ChatOpsController {

    private final ChatOpsService chatOpsService;

    public ChatOpsController(ChatOpsService chatOpsService) {
        this.chatOpsService = chatOpsService;
    }

    @PostMapping
    public String command(@RequestBody String rawInput) {
        return chatOpsService.handle(rawInput);
    }
}
