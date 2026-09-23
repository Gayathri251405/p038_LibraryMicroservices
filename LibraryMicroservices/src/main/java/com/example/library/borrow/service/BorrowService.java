package com.example.library.borrow.service;

import com.example.library.borrow.entity.BorrowRecord;
import com.example.library.borrow.repository.BorrowRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BorrowService {

    private final BorrowRepository borrowRepository;

    public BorrowService(BorrowRepository borrowRepository) {
        this.borrowRepository = borrowRepository;
    }

    // Borrow a book
    public BorrowRecord borrowBook(BorrowRecord borrowRecord) {
        borrowRecord.setBorrowDate(LocalDate.now());
        borrowRecord.setReturned(false);

        return borrowRepository.save(borrowRecord);
    }

    // Get all borrowing records
    public List<BorrowRecord> getAllBorrowRecords() {
        return borrowRepository.findAll();
    }

    // Return a book
    public BorrowRecord returnBook(Long id) {

        BorrowRecord record = borrowRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Borrow record not found"));

        record.setReturned(true);
        record.setReturnDate(LocalDate.now());

        return borrowRepository.save(record);
    }
}