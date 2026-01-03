package com.example.demo.dto;

import lombok.Data;

@Data
public class UserDTO {
    private String keycloakId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String cin;
}