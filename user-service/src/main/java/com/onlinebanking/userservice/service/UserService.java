package com.onlinebanking.userservice.service;

import com.onlinebanking.userservice.model.User;
import com.onlinebanking.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    // Cette méthode sera appelée par l'Auth-Service après la création dans Keycloak
    public void saveProfileFromAuth(String keycloakId, String username, String email, String fName, String lName, String role) {
        User user = new User();
        user.setKeycloakId(keycloakId);
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(fName);
        user.setLastName(lName);
        user.setRole(role);
        userRepository.save(user);
    }
}