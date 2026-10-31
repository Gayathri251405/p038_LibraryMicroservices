package com.example.library.auth.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRole(Role role);

    List<UserAccount> findAllByOrderByUsernameAsc();

    void deleteByUsername(String username);
}
