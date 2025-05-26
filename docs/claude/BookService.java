package com.javabooks.service;

import com.javabooks.model.Book;
import com.javabooks.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

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

    public Optional<Book> findByTitulo(String titulo, Authentication authentication) {
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

    private void validateWriteAccess(Authentication authentication) {
        if (!hasScope(authentication, "libros.write")) {
            throw new AccessDeniedException("No tiene permisos de escritura");
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
    
    public List<Book> findAll(Authentication authentication) {
        validateReadAccess(authentication);
        
        return bookRepository.findAll().stream()
                .filter(book -> hasAccessToCategory(book.getCategoria(), authentication))
                .collect(Collectors.toList());
    }
}