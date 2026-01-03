package com.onlinebanking.userservice.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String keycloakId; // Technical Link to Keycloak

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String cin;

    private String status = "ACTIVE"; // General status (ACTIVE, BLOCKED)


    private String firstName;
    private String lastName;

    // KYC Status (PENDING, VERIFIED, REJECTED)
    private String kycStatus = "PENDING";

    // Personal Info
    private String phoneNumber;
    private String address;
    private String role;

    // Audit Field (Good practice for banking)
    @Column(updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}





