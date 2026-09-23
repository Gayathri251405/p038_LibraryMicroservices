package com.example.library.fine.controller;

import com.example.library.fine.entity.Fine;
import com.example.library.fine.service.FineService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/fines")
public class FineController {

    private final FineService fineService;

    public FineController(FineService fineService) {
        this.fineService = fineService;
    }

    // Create a fine
    @PostMapping
    public Fine createFine(@RequestBody Fine fine) {
        return fineService.createFine(fine);
    }

    // Get all fines
    @GetMapping
    public List<Fine> getAllFines() {
        return fineService.getAllFines();
    }

    // Pay a fine
    @PutMapping("/pay/{id}")
    public Fine payFine(@PathVariable Long id) {
        return fineService.payFine(id);
    }
}