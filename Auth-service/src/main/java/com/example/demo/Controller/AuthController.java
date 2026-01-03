package com.example.demo.Controller;

import com.example.demo.Service.AuthService;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 1. Inscription publique (Force le rôle CLIENT)
    @PostMapping("/register")
    public ResponseEntity<?> registerClient(@RequestBody Map<String, String> request) {
        return registerInternal(request, "CLIENT");
    }

    // 2. Création Staff (Pour créer des AGENTs ou ADMINs)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/create-staff")
    public ResponseEntity<?> createStaff(@RequestBody Map<String, String> request) {
        // On attend un champ "role" dans le JSON (ex: "AGENT")
        String role = request.get("role");
        if (!role.equals("AGENT") && !role.equals("ADMIN")) {
            return ResponseEntity.badRequest().body("Error: Invalid role. Allowed: AGENT, ADMIN");
        }
        return registerInternal(request, role);
    }

    // 3. Login
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            AccessTokenResponse token = authService.login(request.get("username"), request.get("password"));
            return ResponseEntity.ok(token);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Login failed: " + e.getMessage()));
        }
    }

    // 4. Mot de passe oublié
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        try {
            authService.resetPassword(request.get("username"));
            return ResponseEntity.ok(Map.of("message", "Email de réinitialisation envoyé."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Méthode helper privée
    private ResponseEntity<?> registerInternal(Map<String, String> request, String role) {
        try {
            String result = authService.registerUser(
                    request.get("username"),
                    request.get("password"),
                    request.get("email"),
                    request.get("firstName"),
                    request.get("lastName"),
                    role ,
                    request.get("cin")
            );
            return ResponseEntity.ok(Map.of("message", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}