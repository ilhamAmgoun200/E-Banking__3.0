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
    public ResponseEntity<?> register(@RequestBody Map<String, String> user) {
        String username = user.get("username");
        String password = user.get("password");
        String accountNumber = user.get("accountNumber");
        Map<String, Object> resp = new HashMap<>();
        if (username == null || password == null || accountNumber == null) {
            resp.put("success", false);
            resp.put("error", "Username, password, and account number required");
            return ResponseEntity.badRequest().body(resp);
        }
        boolean registered = userService.register(username, password, accountNumber);
        if (registered) {
            resp.put("success", true);
            return ResponseEntity.ok(resp);
        } else {
            resp.put("success", false);
            resp.put("error", "Username or account number already exists");
            return ResponseEntity.badRequest().body(resp);
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
