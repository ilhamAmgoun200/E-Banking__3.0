package com.onlinebanking.userservice.controller;

import com.onlinebanking.userservice.service.UserService;
import com.onlinebanking.userservice.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.onlinebanking.userservice.model.User;

@RestController
@RequestMapping("/api")
public class UserController {
    @Autowired
    private UserService userService;

    // Nouveau endpoint pour la synchronisation avec Auth-Service
    @PostMapping("/profile/create")
    public ResponseEntity<String> createProfile(@RequestBody Map<String, String> data) {
        userService.saveProfileFromAuth(
                data.get("keycloakId"),
                data.get("username"),
                data.get("email"),
                data.get("firstName"),
                data.get("lastName"),
                data.get("role")
        );
        return ResponseEntity.ok("Profil utilisateur synchronisé avec succès");
    }

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("User service opérationnel et lié à Keycloak.");
    }
}