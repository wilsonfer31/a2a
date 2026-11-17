package com.example.orchestrateur.controller;

import com.example.orchestrateur.controller.dto.ChatRequest;
import com.example.orchestrateur.controller.dto.ChatResponse;
import com.example.orchestrateur.service.OrchestratorService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final OrchestratorService orchestratorService;

    public ChatController(OrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String response = orchestratorService.chat(request.message());
        return new ChatResponse(response);
    }
}
