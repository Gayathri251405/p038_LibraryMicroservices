package com.example.fineservice.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fine")
public class FineController {

    @GetMapping
    public List<String> getFines() {
        return List.of("Fine-2001", "Fine-2002");
    }
}
