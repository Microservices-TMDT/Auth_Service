package com.example.auth_service.service.impl;

import com.example.auth_service.dto.request.LoginRequest;
import com.example.auth_service.dto.request.UserRegistrationDto;
import com.example.auth_service.dto.response.TokenResponse;
import com.example.auth_service.exception.AuthException;
import com.example.auth_service.service.UserService;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.auth-server-url}")
    private String serverUrl;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Override
    public void createUser(UserRegistrationDto dto) {
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(dto.getPassword());
        user.setCredentials(Collections.singletonList(credential));

        Response response = keycloak.realm(realm).users().create(user);
        if (response.getStatus() == 409) {
            throw AuthException.conflict("Username hoặc email đã tồn tại");
        }
        if (response.getStatus() != 201) {
            String errorMessage = response.readEntity(String.class);
            log.error("Create user failed - HTTP status: {}, body: {}", response.getStatus(), errorMessage);
            log.error("Keycloak URL: {}, Realm: {}, ClientId: {}", serverUrl, realm, clientId);
            throw AuthException.badRequest("Không thể tạo user trong Keycloak! Status: "
                    + response.getStatus() + " | Error: " + errorMessage);
        }

        log.info("User '{}' created successfully in realm '{}'", dto.getUsername(), realm);
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        try {
            Keycloak userKeycloak = KeycloakBuilder.builder()
                    .serverUrl(serverUrl)
                    .realm(realm)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .username(request.getUsername())
                    .password(request.getPassword())
                    .build();

            AccessTokenResponse tokenResponse = userKeycloak.tokenManager().getAccessToken();
            log.info("User '{}' logged in successfully", request.getUsername());

            return TokenResponse.builder()
                    .accessToken(tokenResponse.getToken())
                    .refreshToken(tokenResponse.getRefreshToken())
                    .tokenType(tokenResponse.getTokenType())
                    .expiresIn(tokenResponse.getExpiresIn())
                    .refreshExpiresIn(tokenResponse.getRefreshExpiresIn())
                    .scope(tokenResponse.getScope())
                    .build();
        } catch (Exception e) {
            log.warn("Login failed for user '{}': {}", request.getUsername(), e.getMessage());
            throw AuthException.unauthorized("Tên đăng nhập hoặc mật khẩu không đúng");
        }
    }
}
