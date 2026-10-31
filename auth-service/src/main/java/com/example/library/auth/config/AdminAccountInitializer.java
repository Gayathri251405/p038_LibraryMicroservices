package com.example.library.auth.config;

import com.example.library.auth.user.Role;
import com.example.library.auth.user.UserAccount;
import com.example.library.auth.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminAccountInitializer {
    @Bean
    CommandLineRunner createDefaultAccounts(UserRepository users, PasswordEncoder encoder,
                                            @Value("${app.admin.username:admin}") String adminUsername,
                                            @Value("${app.admin.password}") String adminPassword,
                                            @Value("${app.librarian.username:librarian}") String librarianUsername,
                                            @Value("${app.librarian.password}") String librarianPassword) {
        return args -> {
            if (!users.existsByUsername(adminUsername)) {
                users.save(new UserAccount(adminUsername, encoder.encode(adminPassword), Role.ADMIN));
            }
            if (!users.existsByUsername(librarianUsername)) {
                users.save(new UserAccount(librarianUsername, encoder.encode(librarianPassword), Role.LIBRARIAN));
            }
        };
    }
}
