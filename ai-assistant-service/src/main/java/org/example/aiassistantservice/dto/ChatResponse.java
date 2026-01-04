package org.example.aiassistantservice.dto;


public class ChatResponse {
    private String answer;

    // Constructeurs
    public ChatResponse() {}

    public ChatResponse(String answer) {
        this.answer = answer;
    }

    // Getters et Setters
    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
