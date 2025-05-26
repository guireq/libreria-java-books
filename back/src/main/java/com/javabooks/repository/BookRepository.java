package com.javabooks.repository;

import com.javabooks.model.Book;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class BookRepository {
    
    private final Map<Long, Book> books = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public BookRepository() {
        // Datos de prueba
        save(new Book(null, "Clean Code", "Robert C. Martin", 464, "PROGRAMMING", "Contenido del libro Clean Code"));
        save(new Book(null, "Effective Java", "Joshua Bloch", 416, "PROGRAMMING", "Contenido del libro Effective Java"));
        save(new Book(null, "The Pragmatic Programmer", "David Thomas", 352, "PROGRAMMING", "Contenido del libro The Pragmatic Programmer"));
        save(new Book(null, "Design Patterns", "Gang of Four", 395, "PROGRAMMING", "Contenido del libro Design Patterns"));
        save(new Book(null, "Spring in Action", "Craig Walls", 520, "FRAMEWORKS", "Contenido del libro Spring in Action"));
        save(new Book(null, "Microservices Patterns", "Chris Richardson", 518, "ARCHITECTURE", "Contenido del libro Microservices Patterns"));
        save(new Book(null, "Domain-Driven Design", "Eric Evans", 560, "ARCHITECTURE", "Contenido del libro Domain-Driven Design"));
    }

    public List<Book> findAll() {
        return new ArrayList<>(books.values());
    }

    public Optional<Book> findById(Long id) {
        return Optional.ofNullable(books.get(id));
    }

    public Optional<Book> findByTitulo(String titulo) {
        return books.values().stream()
                .filter(book -> book.getTitulo().equalsIgnoreCase(titulo))
                .findFirst();
    }

    public List<Book> findByAutor(String autor) {
        return books.values().stream()
                .filter(book -> book.getAutor().equalsIgnoreCase(autor))
                .collect(Collectors.toList());
    }

    public List<Book> findByCategoria(String categoria) {
        return books.values().stream()
                .filter(book -> book.getCategoria().equalsIgnoreCase(categoria))
                .collect(Collectors.toList());
    }

    public Book save(Book book) {
        if (book.getId() == null) {
            book.setId(idGenerator.getAndIncrement());
        }
        books.put(book.getId(), book);
        return book;
    }

    public boolean deleteById(Long id) {
        return books.remove(id) != null;
    }

    public boolean existsById(Long id) {
        return books.containsKey(id);
    }
}