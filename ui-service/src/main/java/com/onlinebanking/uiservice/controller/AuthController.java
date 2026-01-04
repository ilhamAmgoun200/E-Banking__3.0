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
import org.springframework.web.client.RestTemplate;


@Controller
public class AuthController {

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private UserServiceClientWithCircuitBreaker userServiceClientWithCircuitBreaker;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired
    private RestTemplate restTemplate;

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

            // Récupérer le rôle avec RestTemplate au lieu de Feign
            try {
                String accountServiceUrl = "http://localhost:8081/accounts/user/" + username;
                System.out.println("🔍 Fetching accounts from: " + accountServiceUrl);

                Map<String, Object>[] accounts = restTemplate.getForObject(accountServiceUrl, Map[].class);

                System.out.println("🔍 Accounts found: " + (accounts != null ? accounts.length : "null"));

                if (accounts != null && accounts.length > 0) {
                    String role = (String) accounts[0].get("role");
                    session.setAttribute("role", role);

                    System.out.println("✅ Role detected: " + role);

                    // Redirection selon le rôle
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        System.out.println("🔄 Redirecting ADMIN to /admin/accounts");
                        return "redirect:/admin/accounts";
                    } else if ("AGENT".equalsIgnoreCase(role)) {
                        System.out.println("🔄 Redirecting AGENT to /agent/accounts");
                        return "redirect:/agent/accounts";
                    } else {
                        System.out.println("🔄 Redirecting CLIENT to /dashboard");
                        return "redirect:/dashboard";
                    }
                } else {
                    System.out.println("⚠️ No accounts found for user");
                }
            } catch (Exception e) {
                System.err.println("❌ Error fetching user role: " + e.getMessage());
                e.printStackTrace();
            }

            // Fallback si on ne trouve pas le rôle
            System.out.println("⚠️ Fallback to dashboard");
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
    public String register(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String accountNumber,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String cin,
            @RequestParam(required = false, defaultValue = "CLIENT") String role,
            @RequestParam(required = false, defaultValue = "ACTIVE") String status,
            Model model) {

        System.out.println("🔍 Registration - Username: " + username + ", Role: " + role);

        Map<String, String> req = new HashMap<>();
        req.put("username", username);
        req.put("password", password);
        req.put("accountNumber", accountNumber);
        req.put("firstName", firstName);
        req.put("lastName", lastName);
        req.put("email", email);
        req.put("phoneNumber", phoneNumber);
        req.put("address", address);
        req.put("cin", cin);
        req.put("role", role);
        req.put("status", status);

        Map<String, Object> response = userServiceClientWithCircuitBreaker.registerWithFallback(req);

        if (response.containsKey("success") && (Boolean) response.get("success")) {
            // Création compte dans account-service avec RestTemplate
            Map<String, Object> accountReq = new HashMap<>();
            accountReq.put("accountNumber", accountNumber);
            accountReq.put("accountHolderName", username);
            accountReq.put("balance", 0.0);
            accountReq.put("username", username);
            accountReq.put("role", role);
            accountReq.put("status", status);

            System.out.println("📤 Sending to account-service: " + accountReq);

            try {
                String accountServiceUrl = "http://localhost:8081/accounts";
                Map<String, Object> accountResponse = restTemplate.postForObject(
                        accountServiceUrl,
                        accountReq,
                        Map.class
                );
                System.out.println("✅ Account created successfully: " + accountResponse);
            } catch (Exception ex) {
                System.err.println("❌ Account creation failed: " + ex.getMessage());
                ex.printStackTrace();
                model.addAttribute("warning", "User registered but account creation failed. Please contact support.");
            }
            return "redirect:/login";
        } else {
            model.addAttribute("error", response.getOrDefault("error", "Registration failed"));
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
