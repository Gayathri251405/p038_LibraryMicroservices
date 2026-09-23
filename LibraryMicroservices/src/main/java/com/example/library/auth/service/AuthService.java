package com.example.library.auth.service;

import com.example.library.auth.dto.AuthResponse;
import com.example.library.auth.dto.LoginRequest;
import com.example.library.auth.dto.RegisterRequest;
import com.example.library.auth.entity.User;
import com.example.library.auth.repository.UserRepository;
import com.example.library.auth.security.JwtService;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            return "Email already registered";
        }

        String role = request.getRole();

        if (role == null || role.isBlank()) {
            role = "USER";
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());

        // Password is encrypted using BCrypt
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(role.toUpperCase());

        userRepository.save(user);

        return "User registered successfully";
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("Invalid email or password"));

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if (!passwordMatches) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(
                user.getEmail(),
                user.getRole()
        );

        return new AuthResponse(
                token,
                "Login successful"
        );
    }
}