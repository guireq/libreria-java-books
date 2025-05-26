package com.javabooks.repository;

import com.javabooks.model.Usuario;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class UserRepository {
    
    private final Map<String, Usuario> users = new HashMap<>();

    public UserRepository() {
        // Usuarios de prueba
        users.put("admin", new Usuario(
            "admin", 
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG", // password
            Set.of("ADMIN"), 
            Arrays.asList("PROGRAMMING", "FRAMEWORKS", "ARCHITECTURE"), 
            Arrays.asList("Robert C. Martin", "Joshua Bloch", "David Thomas", "Gang of Four", "Craig Walls", "Chris Richardson", "Eric Evans")
        ));
        
        users.put("client1", new Usuario(
            "client1", 
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG", // password
            Set.of("CLIENT"), 
            Arrays.asList("PROGRAMMING"), 
            Arrays.asList("Robert C. Martin", "Joshua Bloch")
        ));
        
        users.put("client2", new Usuario(
            "client2", 
            "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG", // password
            Set.of("CLIENT"), 
            Arrays.asList("FRAMEWORKS", "ARCHITECTURE"), 
            Arrays.asList("Craig Walls", "Chris Richardson", "Eric Evans")
        ));
    }

    public Optional<Usuario> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    public List<Usuario> findAll() {
        return new ArrayList<>(users.values());
    }
}