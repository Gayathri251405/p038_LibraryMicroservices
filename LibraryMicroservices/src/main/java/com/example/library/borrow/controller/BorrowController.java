package com.example.library.borrow.controller;

import com.example.library.borrow.entity.BorrowRecord;
import com.example.library.borrow.service.BorrowService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/borrow")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    // Borrow a book
    @PostMapping
    public BorrowRecord borrowBook(@RequestBody BorrowRecord borrowRecord) {
        return borrowService.borrowBook(borrowRecord);
    }

    // Get all borrowing records
    @GetMapping
    public List<BorrowRecord> getAllBorrowRecords() {
        return borrowService.getAllBorrowRecords();
    }

    // Return a book
    @PutMapping("/return/{id}")
    public BorrowRecord returnBook(@PathVariable Long id) {
        return borrowService.returnBook(id);
    }
}