package com.onlinebanking.uiservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
public class AuditViewController {

    @Value("${audit.service.url:http://localhost:8089}")
    private String auditServiceUrl;

    private final RestTemplate restTemplate;

    public AuditViewController() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Page HTML pour l'interface admin audit
     */
    @GetMapping("/audit")
    public String auditPage(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size) {

        try {
            // Utilisez l'endpoint /api/audit avec page et size
            String url = auditServiceUrl + "/api/audit?page=" + page + "&size=" + size;

            // Créez une réponse temporaire si l'API ne fonctionne pas
            Map<String, Object> response = new HashMap<>();
            response.put("content", new java.util.ArrayList<>());
            response.put("totalPages", 1);
            response.put("totalElements", 0);

            try {
                response = restTemplate.getForObject(url, Map.class);
            } catch (Exception e) {
                // Si l'API échoue, utilisez les valeurs par défaut
                System.err.println("API audit non disponible: " + e.getMessage());
            }

            if (response != null) {
                model.addAttribute("auditLogs", response.get("content"));
                model.addAttribute("currentPage", page);
                model.addAttribute("totalPages", response.get("totalPages"));
                model.addAttribute("totalEvents", response.get("totalElements"));
            }
        } catch (Exception e) {
            model.addAttribute("error", "Impossible de charger les logs d'audit: " + e.getMessage());
        }

        return "admin-audit";
    }

    /**
     * API proxy vers le audit service (pour AJAX)
     */
    @GetMapping("/api/audit-logs")
    @ResponseBody
    public Map<String, Object> getAuditLogsProxy(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            // Construire l'URL correctement
            StringBuilder urlBuilder = new StringBuilder(auditServiceUrl + "/api/audit/search?");
            urlBuilder.append("page=").append(page)
                    .append("&size=").append(size);

            if (userId != null && !userId.isEmpty())
                urlBuilder.append("&userId=").append(userId);
            if (serviceName != null && !serviceName.isEmpty())
                urlBuilder.append("&serviceName=").append(serviceName);
            if (status != null && !status.isEmpty())
                urlBuilder.append("&status=").append(status);
            if (eventType != null && !eventType.isEmpty())
                urlBuilder.append("&eventType=").append(eventType);
            if (from != null && !from.isEmpty())
                urlBuilder.append("&from=").append(from);
            if (to != null && !to.isEmpty())
                urlBuilder.append("&to=").append(to);

            System.out.println("Calling Audit Service: " + urlBuilder.toString());
            return restTemplate.getForObject(urlBuilder.toString(), Map.class);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Service audit indisponible");
            error.put("message", e.getMessage());
            error.put("content", new java.util.ArrayList<>());
            error.put("totalPages", 1);
            error.put("totalElements", 0);
            return error;
        }
    }

    /**
     * API proxy pour les statistiques
     */
    @GetMapping("/api/audit-stats")
    @ResponseBody
    public Map<String, Object> getAuditStatsProxy() {
        try {
            String url = auditServiceUrl + "/api/audit/stats";
            System.out.println("Fetching stats from: " + url);
            return restTemplate.getForObject(url, Map.class);

        } catch (Exception e) {
            System.err.println("Error fetching stats: " + e.getMessage());
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalEvents", 0);
            stats.put("successEvents", 0);
            stats.put("failedEvents", 0);
            stats.put("uniqueServices", 0);
            return stats;
        }
    }
}