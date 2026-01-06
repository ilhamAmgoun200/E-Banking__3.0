package com.onlinebanking.userservice.service;

import com.onlinebanking.userservice.model.User;
import com.onlinebanking.userservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TwoFactorAuthService twoFactorAuthService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public boolean register(Map<String, String> req) {
        try {
            System.out.println("========================================");
            System.out.println("🔵 ENTERING UserService.register()");
            System.out.println("========================================");

            String username = req.get("username");
            String password = req.get("password");
            String accountNumber = req.get("accountNumber");

            System.out.println("📝 RAW VALUES:");
            System.out.println("   username = '" + username + "'");
            System.out.println("   password = " + (password != null ? "PROVIDED (length: " + password.length() + ")" : "NULL"));
            System.out.println("   accountNumber = '" + accountNumber + "'");

            if (username != null) username = username.trim();
            if (accountNumber != null) accountNumber = accountNumber.trim();

            System.out.println("📝 AFTER TRIM:");
            System.out.println("   username = '" + username + "'");
            System.out.println("   accountNumber = '" + accountNumber + "'");

            // Validation des champs
            if (username == null || username.isEmpty()) {
                System.out.println("❌ VALIDATION FAILED: username is null or empty");
                return false;
            }
            if (password == null || password.isEmpty()) {
                System.out.println("❌ VALIDATION FAILED: password is null or empty");
                return false;
            }
            if (accountNumber == null || accountNumber.isEmpty()) {
                System.out.println("❌ VALIDATION FAILED: accountNumber is null or empty");
                return false;
            }

            System.out.println("✅ VALIDATION PASSED: All required fields present");

            // Vérifier si username existe
            System.out.println("🔍 CHECKING IF USERNAME EXISTS IN DATABASE...");
            Optional<User> existing = userRepository.findByUsername(username);
            System.out.println("   Query result: " + (existing.isPresent() ? "FOUND" : "NOT FOUND"));

            if (existing.isPresent()) {
                User existingUser = existing.get();
                System.out.println("❌ USERNAME ALREADY EXISTS!");
                System.out.println("   Existing user details:");
                System.out.println("   - ID: " + existingUser.getId());
                System.out.println("   - Username: " + existingUser.getUsername());
                System.out.println("   - Account Number: " + existingUser.getAccountNumber());
                System.out.println("   - Created At: " + existingUser.getCreatedAt());
                System.out.println("   - Role: " + existingUser.getRole());
                return false;
            }

            System.out.println("✅ USERNAME IS AVAILABLE");
            System.out.println("🔨 CREATING NEW USER OBJECT...");

            // Créer le user
            User user = new User();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setAccountNumber(accountNumber);
            user.setFirstName(req.get("firstName"));
            user.setLastName(req.get("lastName"));
            user.setEmail(req.get("email"));
            user.setPhoneNumber(req.get("phoneNumber"));
            user.setAddress(req.get("address"));
            user.setCin(req.get("cin"));
            user.setRole(req.get("role") != null ? req.get("role") : "CLIENT");
            user.setStatus(req.get("status") != null ? req.get("status") : "ACTIVE");
            user.setCreatedAt(LocalDateTime.now());

            System.out.println("💾 SAVING USER TO DATABASE...");
            User savedUser = userRepository.save(user);

            System.out.println("========================================");
            System.out.println("✅ USER REGISTERED SUCCESSFULLY!");
            System.out.println("   - ID: " + savedUser.getId());
            System.out.println("   - Username: " + savedUser.getUsername());
            System.out.println("   - Account Number: " + savedUser.getAccountNumber());
            System.out.println("========================================");

            return true;

        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            System.out.println("========================================");
            System.out.println("❌ DATABASE CONSTRAINT VIOLATION!");
            System.out.println("   This means username already exists (DB level)");
            System.out.println("   Error: " + e.getMessage());
            System.out.println("========================================");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.out.println("========================================");
            System.out.println("💥 UNEXPECTED ERROR IN UserService.register()");
            System.out.println("   Exception type: " + e.getClass().getName());
            System.out.println("   Message: " + e.getMessage());
            System.out.println("========================================");
            e.printStackTrace();
            return false;
        }
    }

    public Optional<User> authenticate(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }

    public String enable2FA(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User user = userOpt.get();

        if (user.is2faEnabled()) {
            throw new IllegalStateException("2FA already enabled");
        }

        String secret = twoFactorAuthService.generateSecret();
        user.setSecret2fa(secret);
        user.setUsing2fa(true);
        userRepository.save(user);

        return twoFactorAuthService.generateQrUrl(username, secret);
    }

    public boolean disable2FA(String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();

        if (!user.is2faEnabled()) {
            return false;
        }

        user.setSecret2fa(null);
        user.setUsing2fa(null); // or false, but null is fine since nullable
        userRepository.save(user);
        return true;
    }

    public boolean verify2FACode(String username, String code) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("User not found");
        }
        User user = userOpt.get();

        if (!user.is2faEnabled()) {
            return true; // 2FA not enabled (null or false) → skip
        }

        return twoFactorAuthService.verifyCode(user.getSecret2fa(), code);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
}