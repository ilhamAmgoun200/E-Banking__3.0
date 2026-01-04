package com.onlinebanking.uiservice.controller;

import com.onlinebanking.uiservice.service.AccountServiceClientWithCircuitBreaker;
import com.onlinebanking.uiservice.service.TransactionServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminUserController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AccountServiceClientWithCircuitBreaker accountServiceClient;

    @Autowired
    private TransactionServiceClient transactionServiceClient;

    private static final String USER_SERVICE_URL = "http://localhost:8084/api";

    /**
     * Page principale : Liste de tous les utilisateurs avec statistiques
     */
    @GetMapping("/users")
    public String adminUsers(Model model) {
        System.out.println("=== ADMIN USERS PAGE ===");

        try {
            // Récupérer tous les comptes
            List<Map<String, Object>> accounts = accountServiceClient.getAllAccounts();

            if (accounts == null) {
                accounts = new ArrayList<>();
            }

            // Grouper les comptes par username
            Map<String, List<Map<String, Object>>> accountsByUser = accounts.stream()
                    .filter(account -> account.get("username") != null)
                    .collect(Collectors.groupingBy(account -> (String) account.get("username")));

            // Créer une liste d'utilisateurs avec leurs statistiques
            List<Map<String, Object>> usersWithStats = new ArrayList<>();

            for (Map.Entry<String, List<Map<String, Object>>> entry : accountsByUser.entrySet()) {
                String username = entry.getKey();
                List<Map<String, Object>> userAccounts = entry.getValue();

                Map<String, Object> userStats = new HashMap<>();
                userStats.put("username", username);
                userStats.put("accountCount", userAccounts.size());

                // Calculer le solde total
                double totalBalance = userAccounts.stream()
                        .mapToDouble(acc -> {
                            Object balanceObj = acc.get("balance");
                            return balanceObj != null ? ((Number) balanceObj).doubleValue() : 0.0;
                        })
                        .sum();
                userStats.put("totalBalance", totalBalance);

                // Récupérer le nombre de transactions
                try {
                    List<Map<String, Object>> transactions = transactionServiceClient.getTransactionsByUsername(username);
                    userStats.put("transactionCount", transactions != null ? transactions.size() : 0);
                } catch (Exception e) {
                    System.err.println("Erreur récupération transactions pour " + username + ": " + e.getMessage());
                    userStats.put("transactionCount", 0);
                }

                usersWithStats.add(userStats);
            }

            // Trier par username
            usersWithStats.sort(Comparator.comparing(u -> (String) u.get("username")));

            // Calculer les statistiques globales
            int totalUsers = usersWithStats.size();
            int totalAccounts = accounts.size();
            double totalSystemBalance = accounts.stream()
                    .mapToDouble(acc -> {
                        Object balanceObj = acc.get("balance");
                        return balanceObj != null ? ((Number) balanceObj).doubleValue() : 0.0;
                    })
                    .sum();

            model.addAttribute("users", usersWithStats);
            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalAccounts", totalAccounts);
            model.addAttribute("totalSystemBalance", totalSystemBalance);

            return "admin-users";

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors de la récupération des données: " + e.getMessage());
            model.addAttribute("users", new ArrayList<>());
            model.addAttribute("totalUsers", 0);
            model.addAttribute("totalAccounts", 0);
            model.addAttribute("totalSystemBalance", 0.0);
            return "admin-users";
        }
    }

    /**
     * Détails d'un utilisateur spécifique : ses comptes et transactions
     */
    @GetMapping("/users/{username}")
    public String userDetails(@PathVariable String username, Model model) {
        System.out.println("=== ADMIN USER DETAILS: " + username + " ===");

        try {
            // Récupérer les comptes de l'utilisateur
            List<Map<String, Object>> userAccounts = accountServiceClient.getAccountsByUsername(username);

            if (userAccounts == null) {
                userAccounts = new ArrayList<>();
            }

            // Récupérer toutes les transactions de l'utilisateur
            List<Map<String, Object>> allTransactions = transactionServiceClient.getTransactionsByUsername(username);

            if (allTransactions == null) {
                allTransactions = new ArrayList<>();
            }

            // Calculer les statistiques
            double totalBalance = userAccounts.stream()
                    .mapToDouble(acc -> {
                        Object balanceObj = acc.get("balance");
                        return balanceObj != null ? ((Number) balanceObj).doubleValue() : 0.0;
                    })
                    .sum();

            // Compter les transactions par type
            long depositCount = allTransactions.stream()
                    .filter(t -> "DEPOSIT".equals(t.get("type")) || "CREDIT".equals(t.get("type")))
                    .count();

            long withdrawalCount = allTransactions.stream()
                    .filter(t -> "WITHDRAWAL".equals(t.get("type")) || "DEBIT".equals(t.get("type")))
                    .count();

            // Calculer le montant total des dépôts et retraits
            double totalDeposits = allTransactions.stream()
                    .filter(t -> "DEPOSIT".equals(t.get("type")) || "CREDIT".equals(t.get("type")))
                    .mapToDouble(t -> {
                        Object amountObj = t.get("amount");
                        return amountObj != null ? ((Number) amountObj).doubleValue() : 0.0;
                    })
                    .sum();

            double totalWithdrawals = allTransactions.stream()
                    .filter(t -> "WITHDRAWAL".equals(t.get("type")) || "DEBIT".equals(t.get("type")))
                    .mapToDouble(t -> {
                        Object amountObj = t.get("amount");
                        return amountObj != null ? ((Number) amountObj).doubleValue() : 0.0;
                    })
                    .sum();

            // Trier les transactions par date (plus récentes en premier)
            allTransactions.sort((t1, t2) -> {
                try {
                    Object ts1 = t1.get("timestamp");
                    Object ts2 = t2.get("timestamp");
                    if (ts1 != null && ts2 != null) {
                        return ts2.toString().compareTo(ts1.toString());
                    }
                } catch (Exception e) {
                    // Ignorer les erreurs de tri
                }
                return 0;
            });

            model.addAttribute("username", username);
            model.addAttribute("accounts", userAccounts);
            model.addAttribute("transactions", allTransactions);
            model.addAttribute("totalBalance", totalBalance);
            model.addAttribute("accountCount", userAccounts.size());
            model.addAttribute("transactionCount", allTransactions.size());
            model.addAttribute("depositCount", depositCount);
            model.addAttribute("withdrawalCount", withdrawalCount);
            model.addAttribute("totalDeposits", totalDeposits);
            model.addAttribute("totalWithdrawals", totalWithdrawals);

            return "admin-user-details";

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur lors de la récupération des données: " + e.getMessage());
            model.addAttribute("username", username);
            model.addAttribute("accounts", new ArrayList<>());
            model.addAttribute("transactions", new ArrayList<>());
            return "admin-user-details";
        }
    }

    /**
     * Détails d'un compte spécifique avec ses transactions
     */
    @GetMapping("/accounts/{accountNumber}/details")
    public String accountDetails(@PathVariable String accountNumber, Model model) {
        System.out.println("=== ADMIN ACCOUNT DETAILS: " + accountNumber + " ===");

        try {
            // Récupérer les détails du compte
            List<Map<String, Object>> allAccounts = accountServiceClient.getAllAccounts();
            Map<String, Object> account = allAccounts.stream()
                    .filter(acc -> accountNumber.equals(acc.get("accountNumber")))
                    .findFirst()
                    .orElse(null);

            if (account == null) {
                model.addAttribute("error", "Compte non trouvé: " + accountNumber);
                return "admin-account-details";
            }

            // Récupérer les transactions du compte
            List<Map<String, Object>> transactions = transactionServiceClient.getTransactionsByAccountNumber(accountNumber);

            if (transactions == null) {
                transactions = new ArrayList<>();
            }

            // Trier par date
            transactions.sort((t1, t2) -> {
                try {
                    Object ts1 = t1.get("timestamp");
                    Object ts2 = t2.get("timestamp");
                    if (ts1 != null && ts2 != null) {
                        return ts2.toString().compareTo(ts1.toString());
                    }
                } catch (Exception e) {
                    // Ignorer
                }
                return 0;
            });

            model.addAttribute("account", account);
            model.addAttribute("transactions", transactions);
            model.addAttribute("transactionCount", transactions.size());

            return "admin-account-details";

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Erreur: " + e.getMessage());
            return "admin-account-details";
        }
    }
}