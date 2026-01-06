package com.onlinebanking.uiservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import com.onlinebanking.uiservice.service.UserServiceClient;
import com.onlinebanking.uiservice.service.UserServiceClientWithCircuitBreaker;
import com.onlinebanking.uiservice.service.AccountServiceClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
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

    @Autowired
    private RestTemplate restTemplate;
    @GetMapping("/")
    public String index() {
        return "index"; // Assurez-vous que le fichier s'appelle bien index.html
    }
    @GetMapping( "/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            model.addAttribute("name", username);
            model.addAttribute("loginType", "traditional");
        }
        return "dashboard";
    }

    @PostMapping("/login")
    public String loginStep1(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        System.out.println("Login attempt for username: " + username);
        Map<String, String> req = new HashMap<>();
        req.put("username", username);
        req.put("password", password);

        Map<String, Object> response = userServiceClientWithCircuitBreaker.loginWithFallback(req);
        System.out.println("Login response: " + response);

        if (response.containsKey("token")) {
            session.setAttribute("temp_username", username);

            // Check if 2FA is enabled
            try {
                String statusUrl = "http://localhost:8084/api/2fa/status?username=" + username;
                @SuppressWarnings("unchecked")
                Map<String, Object> statusResp = restTemplate.getForObject(statusUrl, Map.class);
                boolean using2fa = (boolean) statusResp.getOrDefault("using2fa", false);

                if (using2fa) {
                    return "2fa-verification";
                } else {
                    completeLogin(session, username);
                    return redirectAccordingToRole(session, username);
                }
            } catch (Exception e) {
                model.addAttribute("error", "Error checking 2FA status: " + e.getMessage());
                return "login";
            }
        } else {
            model.addAttribute("error", response.getOrDefault("error", "Invalid credentials"));
            return "login";
        }
    }

    @PostMapping("/2fa/verify")
    public String verify2FA(
            @RequestParam String code,
            HttpSession session,
            Model model) {

        String username = (String) session.getAttribute("temp_username");
        if (username == null) {
            model.addAttribute("error", "Session expired. Please login again.");
            return "redirect:/login";
        }

        try {
            String verifyUrl = "http://localhost:8084/api/2fa/verify?username=" + username + "&code=" + code;
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = restTemplate.postForObject(verifyUrl, null, Map.class);
            boolean valid = (boolean) resp.getOrDefault("valid", false);

            if (valid) {
                completeLogin(session, username);
                session.removeAttribute("temp_username");
                return redirectAccordingToRole(session, username);
            } else {
                model.addAttribute("error", "Invalid 2FA code");
                return "2fa-verification";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error verifying 2FA: " + e.getMessage());
            return "2fa-verification";
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
            Map<String, Object> accountReq = new HashMap<>();
            accountReq.put("accountNumber", accountNumber);
            accountReq.put("accountHolderName", username);
            accountReq.put("balance", 0.0);
            accountReq.put("username", username);
            accountReq.put("role", role);
            accountReq.put("status", status);

            try {
                String accountServiceUrl = "http://localhost:8081/accounts";
                restTemplate.postForObject(accountServiceUrl, accountReq, Map.class);
            } catch (Exception ex) {
                System.err.println("❌ Account creation failed: " + ex.getMessage());
                ex.printStackTrace();
                model.addAttribute("warning", "User registered but account creation failed.");
            }
            return "redirect:/login";
        } else {
            model.addAttribute("error", response.getOrDefault("error", "Registration failed"));
            return "register";
        }
    }

    @GetMapping("/user-service/circuit-breaker/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getUserServiceCircuitBreakerStatus() {
        Map<String, Object> status = new HashMap<>();
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

    // Helper methods
    private void completeLogin(HttpSession session, String username) {
        session.setAttribute("username", username);
    }

    private String redirectAccordingToRole(HttpSession session, String username) {
        try {
            String accountServiceUrl = "http://localhost:8081/accounts/user/" + username;
            @SuppressWarnings("unchecked")
            Map<String, Object>[] accounts = restTemplate.getForObject(accountServiceUrl, Map[].class);

            if (accounts != null && accounts.length > 0) {
                String role = (String) accounts[0].get("role");
                session.setAttribute("role", role);

                if ("ADMIN".equalsIgnoreCase(role)) {
                    return "redirect:/admin/accounts";
                } else if ("AGENT".equalsIgnoreCase(role)) {
                    return "redirect:/agent/accounts";
                } else {
                    return "redirect:/dashboard";
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching role: " + e.getMessage());
            e.printStackTrace();
        }
        return "redirect:/dashboard";
    }
}