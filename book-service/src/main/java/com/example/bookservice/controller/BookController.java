package com.example.bookservice.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/books")
public class BookController {

    @GetMapping
    public List<String> getBooks() {
        return List.of("Spring in Action", "Clean Code", "Microservices Patterns");
    }
}
