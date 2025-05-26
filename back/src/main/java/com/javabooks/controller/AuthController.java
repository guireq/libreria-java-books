package com.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(
    origins = "*", allowCredentials = "true", 
    allowedHeaders = "*",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS})
public class AuthController {

    @Value("${oauth2.client.client-id:web-client}")
    private String clientId;

    @Value("${oauth2.client.client-secret:web-secret}")
    private String clientSecret;

    @Value("${oauth2.authorization-server:http://localhost:9000}")
    private String authorizationServer;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/token")
    public ResponseEntity<?> exchangeCodeForToken(@RequestBody TokenExchangeRequest request) {
        System.out.println("=== CORS Headers in Request ===");
        System.out.println("=== Token Exchange Request ===");
        System.out.println("Code: " + (request.getCode() != null ? "Present" : "Missing"));
        System.out.println("Redirect URI: " + request.getRedirectUri());
        System.out.println("Code Verifier: " + (request.getCodeVerifier() != null ? "Present" : "Missing"));
        
        // System.out.println("Origin: " + request.getHeader("Origin"));
        // System.out.println("Access-Control-Request-Method: " + request.getHeader("Access-Control-Request-Method"));
        // System.out.println("Access-Control-Request-Headers: " + request.getHeader("Access-Control-Request-Headers"));
        
        try {
            // Validate request
            if (request.getCode() == null || request.getCode().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "invalid_request", "message", "Authorization code is required"));
            }

            // Preparar headers
            String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + credentials);

            // Preparar body
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", request.getCode());
            body.add("redirect_uri", request.getRedirectUri());
            body.add("code_verifier", request.getCodeVerifier());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            // Hacer la petición al Authorization Server
            ResponseEntity<Map> response = restTemplate.postForEntity(
                authorizationServer + "/oauth2/token", 
                entity, 
                Map.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "token_exchange_failed", "message", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshTokenRequest request) {
        try {
            String credentials = Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes());
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "Basic " + credentials);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", request.getRefreshToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                authorizationServer + "/oauth2/token", 
                entity, 
                Map.class
            );

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "refresh_failed", "message", e.getMessage()));
        }
    }

// Manejar preflight requests explícitamente
    @RequestMapping(method = RequestMethod.OPTIONS)
    public ResponseEntity<?> handleOptions() {
        return ResponseEntity.ok()
            .header("Access-Control-Allow-Origin", "*")
            .header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            .header("Access-Control-Allow-Headers", "*")
            .header("Access-Control-Allow-Credentials", "true")
            .header("Access-Control-Max-Age", "3600")
            .build();
    }

    // DTOs
    public static class TokenExchangeRequest {
        private String code;
        private String redirectUri;
        private String codeVerifier;

        // Getters y setters
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        
        public String getCodeVerifier() { return codeVerifier; }
        public void setCodeVerifier(String codeVerifier) { this.codeVerifier = codeVerifier; }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
}