package com.example.library.book.service;

import com.example.library.book.entity.Book;
import com.example.library.book.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    // Add a new book
    public Book addBook(Book book) {
        book.setAvailable(true);
        return bookRepository.save(book);
    }

    // Get all books
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // Get book by ID
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    // Update book
    public Book updateBook(Long id, Book bookDetails) {

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Book not found"));

        book.setTitle(bookDetails.getTitle());
        book.setAuthor(bookDetails.getAuthor());
        book.setCategory(bookDetails.getCategory());
        book.setAvailable(bookDetails.isAvailable());

        return bookRepository.save(book);
    }

    // Delete book
    public void deleteBook(Long id) {
        bookRepository.deleteById(id);
    }
}