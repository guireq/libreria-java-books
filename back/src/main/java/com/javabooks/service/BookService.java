package com.javabooks.service;

import com.javabooks.model.Book;
import com.javabooks.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {

    private static final Logger logger = LoggerFactory.getLogger(BookService.class);

    @Autowired
    private BookRepository bookRepository;
    
    public List<Book> findAll(Authentication authentication) {
        logger.info("=== Authentication Details ===");
        logger.info("Authentication object: {}", authentication);
        logger.info("Principal: {}", authentication.getPrincipal());
        logger.info("Name: {}", authentication.getName());
        logger.info("Authorities: {}", authentication.getAuthorities());
        
        // Si es un JWT, loggear los claims
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            logger.info("JWT Subject: {}", jwt.getSubject());
            logger.info("JWT Claims: {}", jwt.getClaims());
            logger.info("User roles: {}", jwt.getClaimAsStringList("roles"));
            logger.info("User categories: {}", jwt.getClaimAsStringList("categorias"));
            logger.info("User authors: {}", jwt.getClaimAsStringList("autores"));
        }
        
        logger.info("User scopes: {}", authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.joining(", ")));
        
        // Validar que el usu

        // Validar que el usuario tiene permisos de lectura
        validateReadAccess(authentication);
        
        List<Book> books = bookRepository.findAll();
        // if (book.isPresent()) {
        //     validateCategoryAccess(book.get().getCategoria(), authentication);
        //     validateAuthorAccess(book.get().getAutor(), authentication);
        // }
        return books;
    }

    public Optional<Book> findByTitulo(String titulo , Authentication authentication) {
        validateReadAccess(authentication);
        
        Optional<Book> book = bookRepository.findByTitulo(titulo);
        if (book.isPresent()) {
            validateCategoryAccess(book.get().getCategoria(), authentication);
            validateAuthorAccess(book.get().getAutor(), authentication);
        }
        return book;
    }

    public List<Book> findByAutor(String autor, Authentication authentication) {
        validateReadAccess(authentication);
        validateAuthorAccess(autor, authentication);
        
        return bookRepository.findByAutor(autor).stream()
                .filter(book -> hasAccessToCategory(book.getCategoria(), authentication))
                .collect(Collectors.toList());
    }

    public Book save(Book book, Authentication authentication) {
        validateWriteAccess(authentication);
        validateCategoryAccess(book.getCategoria(), authentication);
        validateAuthorAccess(book.getAutor(), authentication);
        return bookRepository.save(book);
    }

    public boolean deleteById(Long id, Authentication authentication) {
        validateWriteAccess(authentication);
        
        Optional<Book> book = bookRepository.findById(id);
        if (book.isPresent()) {
            validateCategoryAccess(book.get().getCategoria(), authentication);
            validateAuthorAccess(book.get().getAutor(), authentication);
            return bookRepository.deleteById(id);
        }
        return false;
    }

    private void validateWriteAccess(Authentication authentication) {
        if (!hasScope(authentication, "libros.write")) {
            throw new AccessDeniedException("No tiene permisos de escritura");
        }
        
        // Verificar que el usuario tiene rol ADMIN para operaciones de escritura
        if (!hasRole(authentication, "ADMIN")) {
            throw new AccessDeniedException("Solo los administradores pueden realizar operaciones de escritura");
        }
    }

    private void validateReadAccess(Authentication authentication) {
        if (!hasScope(authentication, "libros.read")) {
            throw new AccessDeniedException("No tiene permisos de lectura");
        }
    }

    private void validateCategoryAccess(String categoria, Authentication authentication) {
        if (!hasAccessToCategory(categoria, authentication)) {
            throw new AccessDeniedException("No tiene acceso a la categoría: " + categoria);
        }
    }

    private void validateAuthorAccess(String autor, Authentication authentication) {
        if (!hasAccessToAuthor(autor, authentication)) {
            throw new AccessDeniedException("No tiene acceso al autor: " + autor);
        }
    }

    private boolean hasScope(Authentication authentication, String scope) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("SCOPE_" + scope));
    }
    
    private boolean hasRole(Authentication authentication, String role) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        List<String> roles = jwt.getClaimAsStringList("roles");
        return roles != null && roles.contains(role);
    }

    private boolean hasAccessToCategory(String categoria, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        List<String> categorias = jwt.getClaimAsStringList("categorias");
        return categorias != null && categorias.contains(categoria);
    }

    private boolean hasAccessToAuthor(String autor, Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        List<String> autores = jwt.getClaimAsStringList("autores");
        return autores != null && autores.contains(autor);
    }
}