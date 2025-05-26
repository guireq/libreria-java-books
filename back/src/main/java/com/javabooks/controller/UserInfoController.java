package com.javabooks.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class UserInfoController {

    @GetMapping("/userinfo")
    public Map<String, Object> userInfo(Authentication authentication) {
        Map<String, Object> userInfo = new HashMap<>();
        
        if (authentication != null) {
            // Información básica del usuario
            userInfo.put(StandardClaimNames.SUB, authentication.getName());
            userInfo.put(StandardClaimNames.NAME, authentication.getName());
            userInfo.put(StandardClaimNames.PREFERRED_USERNAME, authentication.getName());
            userInfo.put(StandardClaimNames.GIVEN_NAME, authentication.getName());
            userInfo.put(StandardClaimNames.FAMILY_NAME, "");
            userInfo.put(StandardClaimNames.EMAIL, authentication.getName() + "@example.com");
            userInfo.put(StandardClaimNames.EMAIL_VERIFIED, true);
            userInfo.put(StandardClaimNames.UPDATED_AT, Instant.now().getEpochSecond());
            
            // Authorities/Roles
            userInfo.put("authorities", authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
                
            // Información adicional personalizada
            userInfo.put("tenant", "default");
            userInfo.put("department", "IT");
        }
        
        return userInfo;
    }
    
    @GetMapping("/api/user/profile")
    public Map<String, Object> userProfile(Authentication authentication) {
        Map<String, Object> profile = new HashMap<>();
        
        if (authentication != null) {
            profile.put("username", authentication.getName());
            profile.put("authenticated", authentication.isAuthenticated());
            profile.put("authorities", authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        }
        
        return profile;
    }
}
