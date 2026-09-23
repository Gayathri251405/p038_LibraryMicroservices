package com.example.borrowservice.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/borrow")
public class BorrowController {

    @GetMapping
    public List<String> getBorrowRecords() {
        return List.of("Borrow-1001", "Borrow-1002", "Borrow-1003");
    }
}
