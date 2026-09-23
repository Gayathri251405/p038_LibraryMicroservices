package com.example.library.fine.service;

import com.example.library.fine.entity.Fine;
import com.example.library.fine.repository.FineRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class FineService {

    private final FineRepository fineRepository;

    public FineService(FineRepository fineRepository) {
        this.fineRepository = fineRepository;
    }

    // Create a fine
    public Fine createFine(Fine fine) {
        fine.setFineDate(LocalDate.now());
        fine.setPaid(false);

        return fineRepository.save(fine);
    }

    // Get all fines
    public List<Fine> getAllFines() {
        return fineRepository.findAll();
    }

    // Pay a fine
    public Fine payFine(Long id) {

        Fine fine = fineRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fine not found"));

        fine.setPaid(true);

        return fineRepository.save(fine);
    }
}