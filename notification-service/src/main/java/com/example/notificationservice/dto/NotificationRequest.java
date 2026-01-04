package com.onlinebanking.notificationservice.dto;

public class NotificationRequest {
    private String email;
    private String phoneNumber;
    private String message;
    // Default constructor (required for JSON deserialization)
    public NotificationRequest() {
    }

    // Optional constructor for quick creation
    public NotificationRequest(String email, String phoneNumber, String message) {
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.message = message;
    }

    // Getters and setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
