package com.javabooks.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

// @Component
public class JwtCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        Authentication principal = context.getPrincipal();
        
        // Agregar roles al token
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5)) // Remover prefijo "ROLE_"
                .collect(Collectors.toList());
        
        context.getClaims().claim("roles", roles);
        
        // Agregar categorías basadas en el usuario
        List<String> categorias = getCategoriesForUser(principal.getName());
        context.getClaims().claim("categorias", categorias);
        
        // Agregar autores basados en el usuario
        List<String> autores = getAuthorsForUser(principal.getName());
        context.getClaims().claim("autores", autores);
    }
    
    private List<String> getCategoriesForUser(String username) {
        // Lógica para determinar las categorías del usuario
        if ("admin".equals(username)) {
            return Arrays.asList("PROGRAMMING", "FRAMEWORKS", "ARCHITECTURE", "DATABASES");
        } else if ("user".equals(username)) {
            return Arrays.asList("PROGRAMMING", "FRAMEWORKS");
        }
        return Arrays.asList("PROGRAMMING");
    }
    
    private List<String> getAuthorsForUser(String username) {
        // Lógica para determinar los autores del usuario
        if ("admin".equals(username)) {
            return Arrays.asList(
                "Robert C. Martin", 
                "Joshua Bloch", 
                "Craig Walls",
                "Martin Fowler",
                "Eric Evans"
            );
        } else if ("user".equals(username)) {
            return Arrays.asList(
                "Robert C. Martin", 
                "Craig Walls"
            );
        }
        return Arrays.asList("Robert C. Martin");
    }
}