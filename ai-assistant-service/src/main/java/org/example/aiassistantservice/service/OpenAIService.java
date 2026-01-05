package org.example.aiassistantservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;

@Service
public class OpenAIService {

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent";

    // Constructeur
    public OpenAIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public String getChatResponse(String userMessage) {
        // Si pas de clé Gemini, utilise le fallback
        if (geminiApiKey == null || geminiApiKey.isEmpty() || geminiApiKey.startsWith("AIzaSy")) {
            return getFallbackResponse(userMessage);
        }

        try {
            System.out.println("=== Using Gemini API ===");

            // 1. Préparer les headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 2. Créer le prompt bancaire
            String bankingPrompt = """
                Tu es un assistant bancaire expert de E-Bank 3.0.
                Règles STRICTES :
                1. Réponds UNIQUEMENT aux questions bancaires et financières
                2. Domaines autorisés : comptes, cartes, virements, cryptomonnaies (info générale), frais bancaires
                3. Ne donne JAMAIS de conseil d'investissement personnalisé
                4. Pour les questions sur les frais spécifiques : "Veuillez consulter votre espace client pour les frais exacts"
                5. Sois précis, concis et professionnel
                6. Réponds TOUJOURS en français
                7. Si question hors sujet : "Je ne peux répondre qu'aux questions bancaires. Pour d'autres sujets, contactez le service client."
                
                Question de l'utilisateur : "%s"
                
                Réponds comme un vrai assistant bancaire :
                """.formatted(userMessage);

            // 3. Préparer le body JSON
            String requestBody = String.format("""
                {
                    "contents": [{
                        "parts": [{
                            "text": "%s"
                        }]
                    }],
                    "generationConfig": {
                        "temperature": 0.7,
                        "maxOutputTokens": 300
                    }
                }
                """,
                    bankingPrompt.replace("\"", "\\\"")
                            .replace("\n", "\\n")
            );

            // 4. Créer l'entité HTTP
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // 5. Construire l'URL avec la clé API
            String apiUrl = GEMINI_URL + "?key=" + geminiApiKey;
            System.out.println("Calling Gemini API...");

            // 6. Envoyer la requête
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            System.out.println("Gemini Response: " + response.getStatusCode());

            // 7. Parser la réponse JSON
            JsonNode root = objectMapper.readTree(response.getBody());

            // 8. Extraire la réponse textuelle
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    String answer = parts.get(0).path("text").asText();
                    System.out.println("Answer received: " + answer.substring(0, Math.min(50, answer.length())) + "...");
                    return answer;
                }
            }

            // Si structure JSON différente
            return getFallbackResponse(userMessage);

        } catch (Exception e) {
            System.err.println("Gemini API Error: " + e.getMessage());
            e.printStackTrace();
            return getFallbackResponse(userMessage);
        }
    }

    // Méthode fallback améliorée
    private String getFallbackResponse(String userMessage) {
        System.out.println("=== Using Fallback Responses ===");

        String lowerMessage = userMessage.toLowerCase().trim();

        // Base de connaissances bancaire
        Map<String, String> bankingResponses = new HashMap<>();

        bankingResponses.put("ouvrir un compte",
                "**🔓 Ouvrir un compte chez E-Bank 3.0**\n\n" +
                        "📱 **En ligne (recommandé - 100% digital)** :\n" +
                        "1. Téléchargez 'E-Bank 3.0' sur App Store/Google Play\n" +
                        "2. Cliquez sur 'Créer mon compte'\n" +
                        "3. Scannez votre CIN recto/verso\n" +
                        "4. Prenez un selfie pour vérification biométrique\n" +
                        "5. Remplissez vos informations personnelles\n" +
                        "6. Acceptez les conditions générales\n" +
                        "✅ **Compte activé en moins de 24h**\n\n" +
                        "🏢 **En agence** :\n" +
                        "• Prenez RDV sur www.ebank.ma/rdv\n" +
                        "• Documents requis :\n" +
                        "  - Carte nationale d'identité VALIDE\n" +
                        "  - Justificatif de domicile (< 3 mois)\n" +
                        "  - RIB existant (si transfert de compte)\n" +
                        "• Signature électronique des documents\n" +
                        "✅ **Compte immédiatement opérationnel**\n\n" +
                        "🎁 **Inclus GRATUITEMENT** :\n" +
                        "• Carte Visa Classic\n" +
                        "• Application mobile complète\n" +
                        "• Virements nationaux illimités\n" +
                        "• Assistance client 24h/24\n" +
                        "• Assurance perte/vol des moyens de paiement");

        bankingResponses.put("compte courant",
                "**💼 Compte Courant E-Bank 3.0**\n\n" +
                        "✨ **Caractéristiques** :\n" +
                        "• Solde minimum : 100 DH\n" +
                        "• Carte Visa incluse\n" +
                        "• Chéquier sur demande\n" +
                        "• Virements nationaux gratuits\n" +
                        "• Prélèvements automatiques\n" +
                        "• Alertes transactions en temps réel\n\n" +
                        "💰 **Frais mensuels** :\n" +
                        "• Formule Basic : 5 DH/mois\n" +
                        "• Formule Premium : 15 DH/mois\n" +
                        "• Étudiants : GRATUIT\n\n" +
                        "📲 **Gestion** :\n" +
                        "• Consultation solde 24/7\n" +
                        "• Relevés électroniques\n" +
                        "• Historique 5 ans\n" +
                        "• Export PDF/Excel");

        bankingResponses.put("virement",
                "**💸 Service Virements E-Bank**\n\n" +
                        "🇲🇦 **Virement National** :\n" +
                        "• Frais : 0 DH\n" +
                        "• Délai : 24h maximum\n" +
                        "• Plafond : 50,000 DH/jour\n" +
                        "• Disponible : 24/7\n\n" +
                        "🌍 **Virement International** :\n" +
                        "• Frais : 1.5% (minimum 50 DH)\n" +
                        "• Délai : 1-3 jours ouvrés\n" +
                        "• Devises : EUR, USD, GBP\n" +
                        "• Swift/BIC requis\n\n" +
                        "⚡ **Virement Instantané** :\n" +
                        "• Frais : 5 DH\n" +
                        "• Délai : IMMÉDIAT\n" +
                        "• Plafond : 10,000 DH\n" +
                        "• Destinataires : E-Bank uniquement\n\n" +
                        "📱 **Comment faire** :\n" +
                        "Application → Virements → Nouveau virement\n" +
                        "Ou\n" +
                        "Site web → Mon espace → Opérations → Virements");

        bankingResponses.put("crypto",
                "**💰 Portefeuille Cryptomonnaies**\n\n" +
                        "📊 **Actifs disponibles** :\n" +
                        "• Bitcoin (BTC)\n" +
                        "• Ethereum (ETH)\n" +
                        "• Ripple (XRP)\n" +
                        "• Cardano (ADA)\n" +
                        "• Solana (SOL)\n" +
                        "• Polkadot (DOT)\n" +
                        "• 15+ autres cryptomonnaies\n\n" +
                        "🛒 **Comment acheter** :\n" +
                        "1. Allez dans 'Cryptomonnaies' dans l'appli\n" +
                        "2. Choisissez la crypto\n" +
                        "3. Entrez le montant en DH\n" +
                        "4. Confirmez avec code OTP\n" +
                        "✅ **Achat instantané**\n\n" +
                        "💰 **Frais** :\n" +
                        "• Achat : 0.5%\n" +
                        "• Vente : 0.5%\n" +
                        "• Transfert : Selon réseau\n" +
                        "• Dépôt/Retrait DH : Gratuit\n\n" +
                        "🔒 **Sécurité** :\n" +
                        "• 95% en cold storage\n" +
                        "• Assurance jusqu'à 100,000 DH\n" +
                        "• 2FA obligatoire\n" +
                        "• Clés privées cryptées");

        bankingResponses.put("carte bancaire",
                "**💳 Cartes Bancaires E-Bank**\n\n" +
                        "🟡 **Visa Classic** :\n" +
                        "• Coût : Gratuite\n" +
                        "• Plafond retrait : 2,000 DH/jour\n" +
                        "• Plafond paiement : 5,000 DH/jour\n" +
                        "• Assurance : Perte/vol\n\n" +
                        "🔵 **Visa Premium** :\n" +
                        "• Coût : 10 DH/mois\n" +
                        "• Plafond retrait : 5,000 DH/jour\n" +
                        "• Plafond paiement : 15,000 DH/jour\n" +
                        "• Assurance : Étendue (voyage, achats)\n" +
                        "• Accès lounges aéroports\n\n" +
                        "⚫ **Visa Infinite** :\n" +
                        "• Coût : 30 DH/mois\n" +
                        "• Plafonds illimités (sur validation)\n" +
                        "• Assurance complète\n" +
                        "• Service concierge 24/7\n" +
                        "• Programme de fidélité\n\n" +
                        "📱 **Commander** :\n" +
                        "Application → Cartes → Commander une carte\n" +
                        "Livraison sous 5-7 jours ouvrés");

        bankingResponses.put("frais",
                "**💰 Frais et Tarifs E-Bank 3.0**\n\n" +
                        "📋 **Formule Basic (5 DH/mois)** :\n" +
                        "• Virements nationaux : Gratuits\n" +
                        "• Retraits E-Bank : Gratuits\n" +
                        "• Retraits autres DAB : 2 DH\n" +
                        "• Tenue de compte : Inclus\n" +
                        "• Carte Visa Classic : Gratuite\n\n" +
                        "🌟 **Formule Premium (15 DH/mois)** :\n" +
                        "• Tout de Basic PLUS\n" +
                        "• Virements internationaux : -50%\n" +
                        "• Retraits tous DAB : Gratuits\n" +
                        "• Carte Visa Premium : 10 DH/mois\n" +
                        "• Assurance shopping\n" +
                        "• Support prioritaire\n\n" +
                        "👨‍🎓 **Étudiants** :\n" +
                        "• Formule Basic : GRATUITE\n" +
                        "• Carte : GRATUITE\n" +
                        "• Offre valable 5 ans\n\n" +
                        "📊 **Voir vos frais** :\n" +
                        "Application → Mon profil → Mes tarifs\n" +
                        "Ou\n" +
                        "Relevé mensuel détaillé");

        // Recherche de correspondance
        for (Map.Entry<String, String> entry : bankingResponses.entrySet()) {
            if (lowerMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // Vérifications spécifiques
        if (lowerMessage.contains("bonjour") || lowerMessage.contains("salut") || lowerMessage.contains("hello")) {
            return "👋 Bonjour ! Je suis votre assistant bancaire E-Bank 3.0. Comment puis-je vous aider aujourd'hui ?";
        }

        if (lowerMessage.contains("merci")) {
            return "👍 Avec plaisir ! N'hésitez pas si vous avez d'autres questions sur nos services bancaires.";
        }

        if (lowerMessage.contains("au revoir") || lowerMessage.contains("bye")) {
            return "👋 À bientôt ! Pour toute question, je suis disponible 24h/24 dans l'application E-Bank 3.0.";
        }

        // Réponse intelligente par défaut
        return "🤔 **Assistant E-Bank 3.0**\n\n" +
                "Je comprends que vous demandez : \"" + userMessage + "\"\n\n" +
                "Je peux vous aider avec :\n" +
                "• **Ouvrir un compte** (en ligne ou agence)\n" +
                "• **Gérer votre compte courant**\n" +
                "• **Effectuer des virements** (national/international)\n" +
                "• **Cryptomonnaies** (achat/vente/portefeuille)\n" +
                "• **Cartes bancaires** (commande/plafonds)\n" +
                "• **Frais et tarifs**\n\n" +
                "📞 **Besoin d'aide humaine ?**\n" +
                "Service client : 0800 123 456 (gratuit)\n" +
                "Disponible 24h/24, 7j/7\n\n" +
                "Pouvez-vous préciser votre demande ?";
    }
}