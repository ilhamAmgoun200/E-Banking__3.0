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

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("User service is running! JWT implementation is active.");
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> req) {
        Map<String, Object> resp = new HashMap<>();
        try {
            // ✅ LOG LA REQUÊTE REÇUE
            System.out.println("========================================");
            System.out.println("📥 REGISTER REQUEST RECEIVED IN CONTROLLER");
            System.out.println("Request data: " + req);
            System.out.println("========================================");

            boolean registered = userService.register(req);

            System.out.println("========================================");
            System.out.println("📊 REGISTRATION RESULT: " + registered);
            System.out.println("========================================");

            if (registered) {
                resp.put("success", true);
                System.out.println("✅ Sending SUCCESS response to client");
                return ResponseEntity.ok(resp);
            } else {
                resp.put("success", false);
                resp.put("error", "Username or account number already exists or invalid data");
                System.out.println("❌ Sending FAILURE response to client");
                return ResponseEntity.status(409).body(resp);
            }
        } catch (Exception e) {
            System.out.println("========================================");
            System.out.println("💥 EXCEPTION IN REGISTER CONTROLLER");
            System.out.println("Exception: " + e.getClass().getName());
            System.out.println("Message: " + e.getMessage());
            System.out.println("========================================");
            e.printStackTrace();

            resp.put("success", false);
            resp.put("error", "Internal server error: " + e.getMessage());
            return ResponseEntity.status(500).body(resp);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");
        Map<String, Object> resp = new HashMap<>();
        Optional<User> userOpt = userService.authenticate(username, password);
        if (userOpt.isPresent()) {
            String token = jwtUtil.generateToken(username);
            resp.put("token", token);
            return ResponseEntity.ok(resp);
        } else {
            resp.put("error", "Invalid username or password");
            return ResponseEntity.status(401).body(resp);
        }
    }
}