package com.javabooks.controller;

import com.javabooks.model.Book;
import com.javabooks.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/book")
public class BookController {

    @Autowired
    private BookService bookService;

    @GetMapping("/title/{title}")
    public ResponseEntity<?> findByTitle(@PathVariable String title, Authentication authentication) {
        try {
            Optional<Book> book = bookService.findByTitulo(title, authentication);
            if (book.isPresent()) {
                return ResponseEntity.ok(book.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al buscar el libro: " + e.getMessage());
        }
    }

    @GetMapping("/author/{author}")
    public ResponseEntity<?> findByAuthor(@PathVariable String author, Authentication authentication) {
        try {
            List<Book> books = bookService.findByAutor(author, authentication);
            return ResponseEntity.ok(books);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al buscar libros por autor: " + e.getMessage());
        }
    }

    @GetMapping("")
    public ResponseEntity<?> findAll(Authentication authentication) {
        try {
            List<Book> books = bookService.findAll(authentication);
            return ResponseEntity.ok(books);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al buscar el libro: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> createBook(@RequestBody Book book, Authentication authentication) {
        try {
            Book savedBook = bookService.save(book, authentication);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al crear el libro: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id, Authentication authentication) {
        try {
            boolean deleted = bookService.deleteById(id, authentication);
            if (deleted) {
                return ResponseEntity.ok("Libro eliminado correctamente");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Error: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al eliminar el libro: " + e.getMessage());
        }
    }
}