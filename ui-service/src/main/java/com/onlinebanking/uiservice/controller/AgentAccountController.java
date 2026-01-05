package com.onlinebanking.uiservice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.client.ResourceAccessException;
import javax.servlet.http.HttpSession;
import java.util.*;

@Controller
@RequestMapping("/agent")
public class AgentAccountController {

    @Autowired
    private RestTemplate restTemplate;

    private static final String ACCOUNT_SERVICE_URL = "http://localhost:8081/accounts";

    @GetMapping("/accounts")
    public String agentAccounts(Model model, HttpSession session) {
        System.out.println("=== AGENT ACCOUNTS PAGE ===");

        // Vérifier que l'utilisateur est connecté et a le rôle AGENT
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        if (username == null || role == null) {
            System.out.println("❌ Accès refusé : Utilisateur non connecté");
            return "redirect:/login";
        }

        if (!"AGENT".equalsIgnoreCase(role)) {
            System.out.println("❌ Accès refusé : Utilisateur n'est pas AGENT (role: " + role + ")");
            return "redirect:/dashboard";
        }

        System.out.println("✅ Accès autorisé pour AGENT: " + username);

        try {
            // Récupérer tous les comptes
            Map[] accountsArr = restTemplate.getForObject(ACCOUNT_SERVICE_URL, Map[].class);

            // Construire une List<Map<String,Object>> de façon sûre
            List<Map<String, Object>> accounts = new ArrayList<>();
            if (accountsArr != null) {
                for (Map raw : accountsArr) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> typed = (Map<String, Object>) raw;
                    accounts.add(typed);
                }
            }

            // Filtrer uniquement les comptes client pour l'agent
            List<Map<String, Object>> clientAccounts = new ArrayList<>();
            for (Map<String, Object> acc : accounts) {
                Object roleObj = acc.get("role");
                String accountRole = roleObj != null ? roleObj.toString() : "CLIENT";
                if ("CLIENT".equalsIgnoreCase(accountRole)) {
                    clientAccounts.add(acc);
                }
            }

            // Calculer statistiques pour l'espace agent
            int totalClients = clientAccounts.size();
            long totalActive = clientAccounts.stream().filter(a -> "ACTIVE".equals(a.get("status"))).count();
            long totalInactive = clientAccounts.stream().filter(a -> "INACTIVE".equals(a.get("status"))).count();
            double totalBalance = clientAccounts.stream().mapToDouble(a -> {
                Object b = a.get("balance");
                return b != null ? ((Number) b).doubleValue() : 0.0;
            }).sum();

            model.addAttribute("accounts", clientAccounts);
            model.addAttribute("totalClients", totalClients);
            model.addAttribute("totalActive", totalActive);
            model.addAttribute("totalInactive", totalInactive);
            model.addAttribute("totalBalance", totalBalance);

            return "agent-accounts";

        } catch (ResourceAccessException e) {
            System.err.println("Service account-service non disponible: " + e.getMessage());
            model.addAttribute("error", "Le service accounts n'est pas disponible.");
            model.addAttribute("accounts", new ArrayList<>());
            model.addAttribute("totalClients", 0);
            model.addAttribute("totalActive", 0);
            model.addAttribute("totalInactive", 0);
            model.addAttribute("totalBalance", 0.0);
            return "agent-accounts";
        } catch (Exception e) {
            System.err.println("Erreur AgentAccounts: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors de la récupération des comptes: " + e.getMessage());
            model.addAttribute("accounts", new ArrayList<>());
            model.addAttribute("totalClients", 0);
            model.addAttribute("totalActive", 0);
            model.addAttribute("totalInactive", 0);
            model.addAttribute("totalBalance", 0.0);
            return "agent-accounts";
        }
    }

    // Créer un compte — FORCER role = CLIENT, ne pas permettre de définir le solde
    @PostMapping("/accounts/create")
    public String createAccount(@RequestParam String accountNumber,
                                @RequestParam String accountHolderName,
                                @RequestParam String username,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        try {
            Map<String, Object> account = new HashMap<>();
            account.put("accountNumber", accountNumber);
            account.put("accountHolderName", accountHolderName);
            account.put("username", username);
            account.put("role", "CLIENT");          // FORCÉ côté UI et back-end ici
            account.put("status", status);
            account.put("balance", 0.0);            // L'agent ne peut pas définir le solde initial

            restTemplate.postForObject(ACCOUNT_SERVICE_URL, account, Map.class);
            redirectAttributes.addFlashAttribute("message", "Compte client créé avec succès");
        } catch (Exception e) {
            System.err.println("Erreur création compte (agent): " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur création compte: " + e.getMessage());
        }
        return "redirect:/agent/accounts";
    }

    // Modifier compte — NE PAS autoriser modification du solde ni du rôle
    @PostMapping("/accounts/{id}/update")
    public String updateAccount(@PathVariable Long id,
                                @RequestParam String accountNumber,
                                @RequestParam String accountHolderName,
                                @RequestParam String username,
                                @RequestParam String status,
                                RedirectAttributes redirectAttributes) {
        try {
            // Récupérer le compte existant afin de préserver balance et role
            Map existing = restTemplate.getForObject(ACCOUNT_SERVICE_URL + "/" + id, Map.class);
            if (existing == null) {
                redirectAttributes.addFlashAttribute("error", "Compte introuvable");
                return "redirect:/agent/accounts";
            }

            // Mettre à jour uniquement les champs autorisés
            existing.put("accountNumber", accountNumber);
            existing.put("accountHolderName", accountHolderName);
            existing.put("username", username);
            existing.put("status", status);
            // Ne pas changer role ni balance

            restTemplate.put(ACCOUNT_SERVICE_URL + "/" + id, existing);
            redirectAttributes.addFlashAttribute("message", "Compte modifié avec succès");
        } catch (Exception e) {
            System.err.println("Erreur modification compte (agent): " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur modification: " + e.getMessage());
        }
        return "redirect:/agent/accounts";
    }

    // Activer / désactiver un compte (toggle) — accessible aux agents
    @PostMapping("/accounts/{id}/toggle-status")
    public String toggleAccountStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Map account = restTemplate.getForObject(ACCOUNT_SERVICE_URL + "/" + id, Map.class);
            if (account != null) {
                Object roleObj = account.get("role");
                String role = roleObj != null ? roleObj.toString() : "";
                if (!"CLIENT".equalsIgnoreCase(role)) {
                    redirectAttributes.addFlashAttribute("error", "Action non autorisée sur ce rôle");
                    return "redirect:/agent/accounts";
                }

                String currentStatus = (String) account.get("status");
                String newStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "INACTIVE" : "ACTIVE";

                Map<String, String> statusRequest = new HashMap<>();
                statusRequest.put("status", newStatus);

                restTemplate.put(ACCOUNT_SERVICE_URL + "/" + id + "/status", statusRequest);
                redirectAttributes.addFlashAttribute("message", "Statut modifié avec succès");
            } else {
                redirectAttributes.addFlashAttribute("error", "Compte introuvable");
            }
        } catch (Exception e) {
            System.err.println("Erreur changement statut (agent): " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Erreur: " + e.getMessage());
        }
        return "redirect:/agent/accounts";
    }
}