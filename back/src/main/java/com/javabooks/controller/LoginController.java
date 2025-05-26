package com.javabooks.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {

    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                       @RequestParam(value = "logout", required = false) String logout,
                       Model model) {
        
        if (error != null) {
            model.addAttribute("error", "Credenciales inválidas");
        }
        
        if (logout != null) {
            model.addAttribute("message", "Has sido desconectado exitosamente");
        }
        
        // Para debugging - mostrar usuarios disponibles
        model.addAttribute("debugInfo", "Usuarios disponibles: user/password, admin/admin");
        
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }
    
    @GetMapping("/debug/users")
    public String debugUsers(Model model) {
        try {
            var userDetails = userDetailsService.loadUserByUsername("user");
            model.addAttribute("userFound", true);
            model.addAttribute("username", userDetails.getUsername());
            model.addAttribute("authorities", userDetails.getAuthorities());
            
            // Test password encoding
            boolean passwordMatches = passwordEncoder.matches("password", userDetails.getPassword());
            model.addAttribute("passwordMatches", passwordMatches);
            
        } catch (Exception e) {
            model.addAttribute("userFound", false);
            model.addAttribute("error", e.getMessage());
        }
        
        return "debug";
    }
}
