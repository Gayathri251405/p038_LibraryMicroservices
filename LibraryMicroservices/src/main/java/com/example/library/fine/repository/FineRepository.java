package com.example.library.fine.repository;

import com.example.library.fine.entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FineRepository extends JpaRepository<Fine, Long> {

}