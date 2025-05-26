package com.javabooks.model;

import java.util.List;
import java.util.Set;

public class Usuario {
    private String username;
    private String password;
    private Set<String> roles;
    private List<String> categorias;
    private List<String> autores;

    public Usuario() {}

    public Usuario(String username, String password, Set<String> roles, List<String> categorias, List<String> autores) {
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.categorias = categorias;
        this.autores = autores;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public List<String> getCategorias() {
        return categorias;
    }

    public void setCategorias(List<String> categorias) {
        this.categorias = categorias;
    }

    public List<String> getAutores() {
        return autores;
    }

    public void setAutores(List<String> autores) {
        this.autores = autores;
    }
}