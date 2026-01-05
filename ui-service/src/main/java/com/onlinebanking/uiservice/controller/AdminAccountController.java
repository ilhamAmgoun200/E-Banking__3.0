package com.onlinebanking.uiservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminAccountController {

    @Autowired
    private RestTemplate restTemplate;

    private static final String ACCOUNT_SERVICE_URL = "http://localhost:8081/accounts";

    @GetMapping("/accounts")
    public String adminAccounts(Model model, HttpSession session) {
        System.out.println("=== ADMIN ACCOUNTS PAGE ===");

        // Vérifier que l'utilisateur est connecté et a le rôle ADMIN
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        if (username == null || role == null) {
            System.out.println("❌ Accès refusé : Utilisateur non connecté");
            return "redirect:/login";
        }

        if (!"ADMIN".equalsIgnoreCase(role)) {
            System.out.println("❌ Accès refusé : Utilisateur n'est pas ADMIN (role: " + role + ")");
            return "redirect:/dashboard";
        }

        System.out.println("✅ Accès autorisé pour ADMIN: " + username);

        try {
            // Récupérer tous les comptes
            Map[] accounts = restTemplate.getForObject(ACCOUNT_SERVICE_URL, Map[].class);
            System.out.println("Accounts retrieved: " + (accounts != null ? accounts.length : 0));

            // Récupérer les statistiques
            Map<String, Object> stats = restTemplate.getForObject(ACCOUNT_SERVICE_URL + "/stats", Map.class);

            model.addAttribute("accounts", accounts != null ? accounts : new Map[0]);
            model.addAttribute("stats", stats != null ? stats : new HashMap<>());
            return "admin-accounts";

        } catch (ResourceAccessException e) {
            System.err.println("❌ Service non disponible: " + e.getMessage());
            model.addAttribute("error", "Le service account-service n'est pas disponible. Vérifiez qu'il tourne sur le port 8081.");
            model.addAttribute("accounts", new Map[0]);
            model.addAttribute("stats", new HashMap<>());
            return "admin-accounts";

        } catch (Exception e) {
            System.err.println("❌ Erreur inattendue: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();

            model.addAttribute("error", "Erreur: " + e.getMessage());
            model.addAttribute("accounts", new Map[0]);
            model.addAttribute("stats", new HashMap<>());
            return "admin-accounts";
        }
    }

    @PostMapping("/accounts/create")
    public String createAccount(@RequestParam String accountNumber,
                                @RequestParam String accountHolderName,
                                @RequestParam String username,
                                @RequestParam Double balance,
                                @RequestParam String role,
                                @RequestParam String status,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String phoneNumber,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String cin,
                                RedirectAttributes redirectAttributes,
                                HttpSession session) {
        try {
            // Créer le compte dans account-service
            Map<String, Object> account = new HashMap<>();
            account.put("accountNumber", accountNumber);
            account.put("accountHolderName", accountHolderName);
            account.put("username", username);
            account.put("balance", balance);
            account.put("role", role);
            account.put("status", status);

            restTemplate.postForObject(ACCOUNT_SERVICE_URL, account, Map.class);

            // Si c'est un CLIENT, créer aussi l'utilisateur dans user-service avec toutes les infos
            if ("CLIENT".equalsIgnoreCase(role)) {
                try {
                    Map<String, String> userReq = new HashMap<>();
                    userReq.put("username", username);
                    userReq.put("password", "default123"); // Mot de passe par défaut
                    userReq.put("accountNumber", accountNumber);
                    userReq.put("firstName", accountHolderName.split(" ")[0]);
                    userReq.put("lastName", accountHolderName.contains(" ") ? accountHolderName.substring(accountHolderName.indexOf(" ") + 1) : "");
                    userReq.put("email", email != null ? email : "");
                    userReq.put("phoneNumber", phoneNumber != null ? phoneNumber : "");
                    userReq.put("address", address != null ? address : "");
                    userReq.put("cin", cin != null ? cin : "");
                    userReq.put("role", role);
                    userReq.put("status", status);

                    restTemplate.postForObject("http://localhost:8084/api/register", userReq, Map.class);
                } catch (Exception e) {
                    System.err.println("Avertissement: Utilisateur non créé dans user-service: " + e.getMessage());
                }
            }

            redirectAttributes.addFlashAttribute("message", "Compte créé avec succès" +
                    ("CLIENT".equalsIgnoreCase(role) ? " (mot de passe par défaut: default123)" : ""));
        } catch (Exception e) {
            System.err.println("Erreur création: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/accounts/{id}/update")
    public String updateAccount(@PathVariable Long id,
                                @RequestParam String accountNumber,
                                @RequestParam String accountHolderName,
                                @RequestParam String username,
                                @RequestParam Double balance,
                                @RequestParam String role,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> account = new HashMap<>();
            account.put("accountNumber", accountNumber);
            account.put("accountHolderName", accountHolderName);
            account.put("username", username);
            account.put("balance", balance);
            account.put("role", role);
            account.put("status", status);

            restTemplate.put(ACCOUNT_SERVICE_URL + "/" + id, account);
            redirectAttributes.addFlashAttribute("message", "Compte modifié avec succès");
        } catch (Exception e) {
            System.err.println("Erreur modification: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    @PostMapping("/accounts/{id}/delete")
    public String deleteAccount(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            restTemplate.delete(ACCOUNT_SERVICE_URL + "/" + id);
            redirectAttributes.addFlashAttribute("message", "Compte supprimé avec succès");
        } catch (Exception e) {
            System.err.println("Erreur suppression: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/accounts";
    }

    // Changer le statut d'un compte
    @PostMapping("/accounts/{id}/toggle-status")
    public String toggleAccountStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            // Récupérer le compte actuel
            Map account = restTemplate.getForObject(ACCOUNT_SERVICE_URL + "/" + id, Map.class);

            if (account != null) {
                String currentStatus = (String) account.get("status");
                String newStatus = "ACTIVE".equals(currentStatus) ? "INACTIVE" : "ACTIVE";

                Map<String, String> statusRequest = new HashMap<>();
                statusRequest.put("status", newStatus);

                restTemplate.put(ACCOUNT_SERVICE_URL + "/" + id + "/status", statusRequest);
                redirectAttributes.addFlashAttribute("message", "Statut modifié avec succès");
            }
        } catch (Exception e) {
            System.err.println("Erreur changement statut: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/admin/accounts";
    }
}