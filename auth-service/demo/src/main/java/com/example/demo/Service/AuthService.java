package com.example.demo.Service;

import com.example.demo.client.UserServiceClient;
import com.example.demo.dto.UserDTO;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class AuthService {

    private final Keycloak keycloak;
    private final RestTemplate restTemplate;
    private final UserServiceClient userServiceClient; // <--- 1. AJOUT DU CLIENT

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.app-client-id}")
    private String appClientId;

    @Value("${keycloak.app-client-secret}")
    private String appClientSecret;

    // 2. MODIFICATION DU CONSTRUCTEUR POUR INJECTER LE CLIENT
    public AuthService(Keycloak keycloak, UserServiceClient userServiceClient) {
        this.keycloak = keycloak;
        this.userServiceClient = userServiceClient;
        this.restTemplate = new RestTemplate();
    }

    public String registerUser(String username, String password, String email, String firstName, String lastName, String roleName) {

        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        user.setEmailVerified(true);

        // MFA Configuration
        user.setRequiredActions(List.of("CONFIGURE_TOTP"));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        user.setCredentials(Collections.singletonList(credential));

        UsersResource usersResource = keycloak.realm(realm).users();
        Response response = usersResource.create(user);

        if (response.getStatus() == 201) {
            String userId = CreatedResponseUtil.getCreatedId(response);

            // Assignation du rôle
            assignRole(userId, roleName);

            // --- 3. SYNCHRONISATION AVEC USER-SERVICE ---
            try {
                UserDTO userDTO = new UserDTO();
                userDTO.setKeycloakId(userId); // Le lien crucial
                userDTO.setUsername(username);
                userDTO.setEmail(email);
                userDTO.setFirstName(firstName);
                userDTO.setLastName(lastName);
                userDTO.setRole(roleName);

                // L'appel "téléphonique" vers l'autre service
                userServiceClient.createUserProfile(userDTO);
                System.out.println("✅ Succès : Profil créé dans User-Service pour l'ID " + userId);

            } catch (Exception e) {
                // On log l'erreur mais on ne bloque pas l'inscription Keycloak
                System.err.println("⚠️ ATTENTION : Échec de la synchro User-Service : " + e.getMessage());
            }
            // ---------------------------------------------

            return "Utilisateur créé avec succès (MFA requis). ID: " + userId;
        } else {
            throw new RuntimeException("Erreur Keycloak: " + response.getStatusInfo());
        }
        // 2. APPEL AU USER-SERVICE (C'est ici que le lien se fait)
        UserDTO userDto = new UserDTO();
        userDto.setKeycloakId(keycloakId);
        userDto.setUsername(username);
        userDto.setEmail(email);
        userDto.setFirstName(firstName);
        userDto.setLastName(lastName);
        userDto.setRole(role);

        userServiceClient.createUserProfile(userDto); // Utilise Feign Client

        return "Utilisateur créé avec succès";

    }

    public AccessTokenResponse login(String username, String password) {
        String tokenUrl = serverUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", appClientId);
        map.add("client_secret", appClientSecret);
        map.add("username", username);
        map.add("password", password);
        map.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        return restTemplate.postForObject(tokenUrl, request, AccessTokenResponse.class);
    }

    public void resetPassword(String username) {
        UsersResource usersResource = keycloak.realm(realm).users();
        List<UserRepresentation> users = usersResource.search(username);

        if (users.isEmpty()) {
            throw new RuntimeException("Utilisateur introuvable");
        }
        usersResource.get(users.get(0).getId()).executeActionsEmail(List.of("UPDATE_PASSWORD"));
    }

    private void assignRole(String userId, String roleName) {
        RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
        keycloak.realm(realm).users().get(userId).roles().realmLevel().add(Collections.singletonList(role));
    }
}