package org.example.aiassistantservice.dto;


public class ChatRequest {
    private String message;

    // Constructeurs
    public ChatRequest() {}

    public ChatRequest(String message) {
        this.message = message;
    }

    // Getters et Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}