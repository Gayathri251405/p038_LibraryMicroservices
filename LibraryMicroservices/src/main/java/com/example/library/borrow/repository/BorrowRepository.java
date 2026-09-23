package com.example.library.borrow.repository;

import com.example.library.borrow.entity.BorrowRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowRepository extends JpaRepository<BorrowRecord, Long> {

}