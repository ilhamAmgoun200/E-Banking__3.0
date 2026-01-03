package com.onlinebanking.uiservice.controller;

import com.onlinebanking.uiservice.service.AccountServiceClient;
import com.onlinebanking.uiservice.service.UserServiceClient;
import com.onlinebanking.uiservice.service.UserServiceClientWithCircuitBreaker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private UserServiceClientWithCircuitBreaker userServiceClientWithCircuitBreaker;

    @Autowired
    private AccountServiceClient accountServiceClient;

    /**
     * PAGE D'ACCUEIL
     */
    @GetMapping("/")
    public String home() {
        return "home";
    }

    /**
     * PAGE DE LOGIN
     * @param type optionnel pour adapter le style (ex: /login?type=agent)
     */
    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "type", required = false) String type, Model model) {
        model.addAttribute("loginType", type != null ? type : "client");
        return "login";
    }

    /**
     * TRAITEMENT DU LOGIN
     */
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        Model model,
                        HttpSession session) {

        System.out.println("Tentative de connexion : " + username);

        Map<String, String> req = new HashMap<>();
        req.put("username", username);
        req.put("password", password);

        // Appel au microservice Auth via le Circuit Breaker
        Map<String, Object> response = userServiceClientWithCircuitBreaker.loginWithFallback(req);

        if (response.containsKey("token")) {
            // 1. Stockage de la session
            session.setAttribute("username", username);
            session.setAttribute("token", response.get("token"));

            // 2. Récupération et stockage du rôle
            // On vérifie si le rôle vient de la réponse, sinon ROLE_USER par défaut
            String userRole = (String) response.getOrDefault("role", "ROLE_USER");
            session.setAttribute("role", userRole);

            System.out.println("Login réussi. Utilisateur: " + username + " | Rôle: " + userRole);

            // Redirection intelligente selon le rôle
            if (isAdminOrAgent(userRole)) {
                return "redirect:/admin/portal";
            }
            return "redirect:/dashboard";
        } else {
            handleLoginError(response, model);
            return "login";
        }
    }

    /**
     * DASHBOARD CLIENT (Style Mauve)
     */
    @GetMapping("/dashboard")
    public String clientDashboard(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username == null) return "redirect:/login";

        model.addAttribute("name", username);
        model.addAttribute("role", session.getAttribute("role"));
        return "dashboard"; // Vue Client
    }

    /**
     * PORTAIL AGENT / ADMIN (Style Dark Navy)
     */
    @GetMapping("/admin/portal")
    public String adminPortal(Model model, HttpSession session) {
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        if (username == null || !isAdminOrAgent(role)) {
            return "redirect:/login?type=agent";
        }

        model.addAttribute("name", username);
        model.addAttribute("role", role);
        return "admin-dashboard"; // Vue Agent/Admin
    }

    /**
     * PAGE D'INSCRIPTION
     */
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    /**
     * TRAITEMENT DE L'INSCRIPTION
     */
    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String accountNumber,
                           Model model) {

        Map<String, String> req = new HashMap<>();
        req.put("username", username);
        req.put("password", password);
        req.put("accountNumber", accountNumber);

        Map<String, Object> response = userServiceClientWithCircuitBreaker.registerWithFallback(req);

        if (response.containsKey("success") && (Boolean) response.get("success")) {
            // Création du compte bancaire initial dans Account-Service
            createInitialAccount(username, accountNumber, model);
            return "redirect:/login?registered=true";
        } else {
            handleRegistrationError(response, model);
            return "register";
        }
    }

    /**
     * LOGOUT
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    // --- MÉTHODES PRIVÉES (HELPERS) ---

    private boolean isAdminOrAgent(String role) {
        return "ROLE_ADMIN".equals(role) || "ROLE_AGENT".equals(role);
    }

    private void createInitialAccount(String username, String accountNumber, Model model) {
        Map<String, Object> accountReq = new HashMap<>();
        accountReq.put("accountNumber", accountNumber);
        accountReq.put("accountHolderName", username);
        accountReq.put("balance", 0.0);
        accountReq.put("username", username);
        try {
            accountServiceClient.createAccount(accountReq);
        } catch (Exception ex) {
            System.err.println("Erreur création compte: " + ex.getMessage());
            model.addAttribute("warning", "Compte créé, mais l'initialisation bancaire a échoué.");
        }
    }

    private void handleLoginError(Map<String, Object> response, Model model) {
        if (Boolean.TRUE.equals(response.get("circuitBreakerOpen"))) {
            model.addAttribute("error", "🔴 Service d'authentification indisponible (Maintenance)");
        } else {
            model.addAttribute("error", response.getOrDefault("error", "Nom d'utilisateur ou mot de passe incorrect"));
        }
    }

    private void handleRegistrationError(Map<String, Object> response, Model model) {
        if (Boolean.TRUE.equals(response.get("circuitBreakerOpen"))) {
            model.addAttribute("error", "🔴 Le service d'inscription est saturé. Réessayez plus tard.");
        } else {
            model.addAttribute("error", response.getOrDefault("error", "Erreur lors de la création du compte"));
        }
    }
}