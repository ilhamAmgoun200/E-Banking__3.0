package com.onlinebanking.uiservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.onlinebanking.uiservice.service.UserServiceClient;
import com.onlinebanking.uiservice.service.UserServiceClientWithCircuitBreaker;
import com.onlinebanking.uiservice.service.AccountServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private UserServiceClientWithCircuitBreaker userServiceClientWithCircuitBreaker;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @GetMapping({"/", "/login"})
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        // Traditional login user
        String username = (String) session.getAttribute("username");
        if (username != null) {
            model.addAttribute("name", username);
            model.addAttribute("loginType", "traditional");
        }
        return "dashboard";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model, HttpSession session) {
        System.out.println("Login attempt for username: " + username);
        
        Map<String, String> req = new HashMap<>();
        req.put("username", username);
        req.put("password", password);
        
        // Use circuit breaker for login
        Map<String, Object> response = userServiceClientWithCircuitBreaker.loginWithFallback(req);
        
        System.out.println("Login response: " + response);
        
        if (response.containsKey("token")) {
            // Store username in session
            session.setAttribute("username", username);
            System.out.println("Login successful for: " + username);
            return "redirect:/dashboard";
        } else {
            // Handle circuit breaker open state with special message
            if (response.containsKey("circuitBreakerOpen") && (Boolean) response.get("circuitBreakerOpen")) {
                model.addAttribute("error", "🔴 " + response.get("error"));
                model.addAttribute("circuitBreakerError", true);
                System.out.println("Circuit breaker error for: " + username);
            } else {
                model.addAttribute("error", response.getOrDefault("error", "Login failed"));
                System.out.println("Login failed for: " + username + " - " + response.getOrDefault("error", "Login failed"));
            }
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, @RequestParam String accountNumber, Model model) {
        Map<String, String> req = new HashMap<>();
        req.put("username", username);
        req.put("password", password);
        req.put("accountNumber", accountNumber);
        
        // Use circuit breaker for registration
        Map<String, Object> response = userServiceClientWithCircuitBreaker.registerWithFallback(req);
        
        if (response.containsKey("success") && (Boolean)response.get("success")) {
            // Create account in account-service
            Map<String, Object> accountReq = new HashMap<>();
            accountReq.put("accountNumber", accountNumber);
            accountReq.put("accountHolderName", username);
            accountReq.put("balance", 0.0);
            accountReq.put("username", username);
            try {
                accountServiceClient.createAccount(accountReq);
            } catch (Exception ex) {
                // Optionally log or handle account creation failure
                model.addAttribute("warning", "User registered but account creation failed. Please contact support.");
            }
            return "redirect:/login";
        } else {
            // Handle circuit breaker open state with special message
            if (response.containsKey("circuitBreakerOpen") && (Boolean) response.get("circuitBreakerOpen")) {
                model.addAttribute("error", "🔴 " + response.get("error"));
                model.addAttribute("circuitBreakerError", true);
            } else {
                model.addAttribute("error", response.getOrDefault("error", "Registration failed"));
            }
            return "register";
        }
    }

    // Circuit breaker monitoring endpoint for user service
    @GetMapping("/user-service/circuit-breaker/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserServiceCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Get user service circuit breaker status
        String state = userServiceClientWithCircuitBreaker.getCircuitBreakerState();
        io.github.resilience4j.circuitbreaker.CircuitBreaker.Metrics metrics = userServiceClientWithCircuitBreaker.getCircuitBreakerMetrics();
        
        Map<String, Object> userServiceStatus = new HashMap<>();
        userServiceStatus.put("state", state);
        userServiceStatus.put("failureRate", metrics.getFailureRate());
        userServiceStatus.put("numberOfBufferedCalls", metrics.getNumberOfBufferedCalls());
        userServiceStatus.put("numberOfFailedCalls", metrics.getNumberOfFailedCalls());
        userServiceStatus.put("numberOfSuccessfulCalls", metrics.getNumberOfSuccessfulCalls());
        
        status.put("user-service", userServiceStatus);
        status.put("timestamp", java.time.LocalDateTime.now());
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}
