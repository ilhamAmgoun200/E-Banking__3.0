package com.onlinebanking.userservice.controller;

import com.onlinebanking.userservice.model.User;
import com.onlinebanking.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    // 1. Création de profil (Appelé par Auth-Service ou lors de l'inscription)
    // Pas de restriction stricte ici car c'est une création initiale
    @PostMapping("/create-profile")
    public ResponseEntity<?> createProfile(@RequestBody User user,@RequestHeader("X-Internal-Secret") String secret) {
        // 1. Vérification simple si l'utilisateur existe déjà
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("L'utilisateur existe déjà");
        }

        // 2. Initialisation des valeurs par défaut
        user.setKycStatus("PENDING");

        // 3. Sauvegarde
        User savedUser = userRepository.save(user);

        return ResponseEntity.ok(savedUser);
    }
    // 2. Récupérer un profil
    // Autorisé si : C'est MON profil OU je suis un membre du staff (AGENT/ADMIN)
    @GetMapping("/{keycloakId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('AGENT') or #keycloakId == authentication.name")
    public ResponseEntity<?> getUserProfile(@PathVariable String keycloakId) {
        Optional<User> user = userRepository.findByKeycloakId(keycloakId);
        if (user.isPresent()) {
            return ResponseEntity.ok(user.get());
        }
        return ResponseEntity.notFound().build();
    }

    // 3. Mettre à jour ses infos personnelles
    // Autorisé si : C'est MON profil uniquement (on évite qu'un agent modifie vos infos perso sans trace)
    @PutMapping("/{keycloakId}")
    @PreAuthorize("#keycloakId == authentication.name")
    public ResponseEntity<?> updateProfile(@PathVariable String keycloakId, @RequestBody User userUpdates) {
        Optional<User> userOpt = userRepository.findByKeycloakId(keycloakId);
        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();
            // On ne permet pas de modifier l'ID ou le Username ici, juste les infos métier
            existingUser.setPhoneNumber(userUpdates.getPhoneNumber());
            existingUser.setFirstName(userUpdates.getFirstName());
            existingUser.setLastName(userUpdates.getLastName());
            existingUser.setAddress(userUpdates.getAddress());
            existingUser.setEmail(userUpdates.getEmail());

            userRepository.save(existingUser);
            return ResponseEntity.ok(existingUser);
        }
        return ResponseEntity.notFound().build();
    }

    // --- NOUVEAUX ENDPOINTS POUR LE PERSONNEL (AGENT / ADMIN) ---

    // 4. Lister tous les clients (Pour le tableau de bord Agent)
    @GetMapping("/all")
    @PreAuthorize("hasRole('AGENT') or hasRole('ADMIN')")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // 5. Valider le KYC d'un client (Réservé aux AGENTS)
    @PutMapping("/{keycloakId}/validate-kyc")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<?> validateKyc(@PathVariable String keycloakId, @RequestParam String status) {
        Optional<User> userOpt = userRepository.findByKeycloakId(keycloakId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // On accepte seulement "VERIFIED" ou "REJECTED"
            if (status.equalsIgnoreCase("VERIFIED") || status.equalsIgnoreCase("REJECTED")) {
                user.setKycStatus(status.toUpperCase());
                userRepository.save(user);
                return ResponseEntity.ok("KYC status updated to " + status);
            } else {
                return ResponseEntity.badRequest().body("Invalid status. Use VERIFIED or REJECTED");
            }
        }
        return ResponseEntity.notFound().build();
    }
}