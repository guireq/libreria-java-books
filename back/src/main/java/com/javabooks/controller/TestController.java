package com.javabooks.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/public")
    public ResponseEntity<Map<String, Object>> publicEndpoint() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Este es un endpoint público");
        response.put("timestamp", LocalDateTime.now());
        response.put("access", "public");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/protected")
    public ResponseEntity<Map<String, Object>> protectedEndpoint(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Este es un endpoint protegido");
        response.put("user", auth.getName());
        response.put("timestamp", LocalDateTime.now());
        response.put("authorities", auth.getAuthorities());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> adminEndpoint(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Este endpoint requiere rol ADMIN");
        response.put("user", auth.getName());
        response.put("timestamp", LocalDateTime.now());
        response.put("authorities", auth.getAuthorities());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> serverInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("server", "OAuth2 Authorization Server");
        info.put("version", "1.0.0");
        info.put("timestamp", LocalDateTime.now());
        info.put("endpoints", Map.of(
            "authorization", "/oauth2/authorize",
            "token", "/oauth2/token",
            "userinfo", "/userinfo",
            "jwks", "/oauth2/jwks",
            "introspect", "/oauth2/introspect"
        ));
        return ResponseEntity.ok(info);
    }
}
