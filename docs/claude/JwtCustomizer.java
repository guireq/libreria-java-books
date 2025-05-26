package com.javabooks.security;

import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
public class JwtCustomizer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {
        if (context.getPrincipal().getPrincipal() instanceof CustomUserDetails userDetails) {
            // Agregar claims personalizados
            context.getClaims().claim("categorias", userDetails.getCategorias());
            context.getClaims().claim("autores", userDetails.getAutores());
            
            // Agregar roles
            Set<String> roles = userDetails.getUsuario().getRoles();
            context.getClaims().claim("roles", roles);
            
            // Agregar scopes basados en roles
            if (roles.contains("ADMIN")) {
                context.getClaims().claim("scope", "libros.read libros.write");
            } else if (roles.contains("CLIENT")) {
                context.getClaims().claim("scope", "libros.read");
            }
        }
    }
}