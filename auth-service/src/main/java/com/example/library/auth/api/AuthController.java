package com.example.library.auth.api;

import com.example.library.auth.user.Role;
import com.example.library.auth.user.UserAccount;
import com.example.library.auth.user.UserRepository;
import com.example.library.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody Credentials credentials) {
        if (users.existsByUsername(credentials.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }
        try {
            UserAccount user = users.saveAndFlush(new UserAccount(
                    credentials.username(), passwordEncoder.encode(credentials.password()), Role.STUDENT));
            return responseFor(user);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody Credentials credentials) {
        UserAccount user = users.findByUsername(credentials.username())
                .filter(account -> passwordEncoder.matches(credentials.password(), account.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return responseFor(user);
    }

    @GetMapping("/users")
    public List<UserView> listUsers() {
        return users.findAllByOrderByUsernameAsc().stream()
                .map(user -> new UserView(user.getUsername(), user.getRole().name()))
                .toList();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView createUser(@Valid @RequestBody ManagedAccountRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }
        Role role = managedRole(request.role());
        try {
            UserAccount user = users.saveAndFlush(new UserAccount(
                    request.username(), passwordEncoder.encode(request.password()), role));
            return new UserView(user.getUsername(), user.getRole().name());
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already registered");
        }
    }

    @PutMapping("/users/{username}/role")
    public UserView updateRole(@PathVariable String username, @Valid @RequestBody RoleRequest request,
                               Authentication authentication) {
        if (authentication.getName().equals(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators cannot change their own role");
        }
        UserAccount user = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        Role newRole = managedRole(request.role());
        if (user.getRole() == Role.ADMIN && newRole != Role.ADMIN && users.countByRole(Role.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The last administrator cannot be demoted");
        }
        user.setRole(newRole);
        users.save(user);
        return new UserView(user.getUsername(), user.getRole().name());
    }

    @DeleteMapping("/users/{username}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable String username, Authentication authentication) {
        if (authentication.getName().equals(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators cannot delete their own account");
        }
        UserAccount user = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == Role.ADMIN && users.countByRole(Role.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The last administrator cannot be deleted");
        }
        users.deleteByUsername(username);
    }

    private Role managedRole(String role) {
        try {
            Role parsed = Role.valueOf(role);
            if (parsed == Role.ADMIN || parsed == Role.USER) {
                throw new IllegalArgumentException();
            }
            return parsed;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Role must be STUDENT, FACULTY, or LIBRARIAN");
        }
    }

    private AuthResponse responseFor(UserAccount user) {
        return new AuthResponse(jwtService.createToken(user.getUsername(), user.getRole().name()),
                "Bearer", user.getUsername(), user.getRole().name());
    }

    public record Credentials(@NotBlank @Size(max = 80) String username,
                              @NotBlank @Size(min = 8, max = 72) String password) {
    }

    public record AuthResponse(String accessToken, String tokenType, String username, String role) {
    }

    public record ManagedAccountRequest(@NotBlank @Size(max = 80) String username,
                                        @NotBlank @Size(min = 8, max = 72) String password,
                                        @NotBlank String role) {
    }

    public record RoleRequest(@NotBlank String role) {
    }

    public record UserView(String username, String role) {
    }
}
