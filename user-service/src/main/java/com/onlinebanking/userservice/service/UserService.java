package com.onlinebanking.userservice.service;

import com.onlinebanking.userservice.model.User;
import com.onlinebanking.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

    @Service
    public class UserService {
        @Autowired
        private UserRepository userRepository;

        // Create a user (called after Keycloak registration)
        public User createUser(User user) {
            return userRepository.save(user);
        }

        // Find by Keycloak ID
        public Optional<User> getUserByKeycloakId(String keycloakId) {
            return userRepository.findByKeycloakId(keycloakId);
        }

        // Find all (for Agents)
        public List<User> getAllUsers() {
            return userRepository.findAll();
        }
    }

