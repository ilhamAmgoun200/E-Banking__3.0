package org.example.aiassistantservice.controller;

import org.example.aiassistantservice.dto.ChatRequest;
import org.example.aiassistantservice.dto.ChatResponse;
import org.example.aiassistantservice.service.OpenAIService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private OpenAIService openAIService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        String answer = openAIService.getChatResponse(request.getMessage());
        ChatResponse response = new ChatResponse(answer);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Assistant Service is running!");
    }
}
