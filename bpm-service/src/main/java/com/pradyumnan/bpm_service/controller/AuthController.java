package com.pradyumnan.bpm_service.controller;

import com.pradyumnan.bpm_service.model.AppUser;
import com.pradyumnan.bpm_service.model.Role;
import com.pradyumnan.bpm_service.model.UserStatus;
import com.pradyumnan.bpm_service.repository.UserRepository;
import com.pradyumnan.bpm_service.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record Credentials(String username, String password) {}

    public record PasswordChange(String currentPassword, String newPassword) {}

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCK_MINUTES = 15;
    private static final Pattern USERNAME_PATTERN = Pattern.compile("[A-Za-z0-9._-]{3,30}");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Credentials creds) {
        if (creds == null || creds.username() == null || !USERNAME_PATTERN.matcher(creds.username()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Username must be 3 to 30 characters: letters, numbers, dot, dash or underscore"));
        }
        if (creds.password() == null || creds.password().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters"));
        }
        if (userRepository.findByUsername(creds.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already taken"));
        }

        // New accounts cannot sign in until an administrator approves them
        AppUser user = new AppUser();
        user.setUsername(creds.username());
        user.setPasswordHash(passwordEncoder.encode(creds.password()));
        user.setRole(Role.VIEWER);
        user.setStatus(UserStatus.PENDING);
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message",
                "Account created. An administrator must approve it before you can sign in."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credentials creds) {
        AppUser user = (creds == null || creds.username() == null)
                ? null
                : userRepository.findByUsername(creds.username()).orElse(null);
        if (user == null) {
            return invalidLogin();
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error",
                    "Too many failed attempts. Try again in a few minutes."));
        }

        if (creds.password() == null || !passwordEncoder.matches(creds.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
                attempts = 0;
            }
            user.setFailedLoginAttempts(attempts);
            userRepository.save(user);
            return invalidLogin();
        }

        // Only after the password is correct do we say anything about the account state,
        // so the messages below cannot be used to discover which usernames exist.
        if (user.getStatus() == UserStatus.PENDING) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error",
                    "Your account is awaiting administrator approval."));
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error",
                    "This account has been disabled. Contact an administrator."));
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", jwtService.generateToken(user.getUsername()));
        body.put("role", user.getRole().name());
        body.put("mustChangePassword", user.isMustChangePassword());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .<ResponseEntity<?>>map(u -> {
                    Map<String, Object> body = new LinkedHashMap<>();
                    body.put("username", u.getUsername());
                    body.put("role", u.getRole().name());
                    body.put("mustChangePassword", u.isMustChangePassword());
                    return ResponseEntity.ok(body);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication authentication, @RequestBody PasswordChange request) {
        AppUser user = userRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // 400 (not 401) for a wrong current password, so the frontend does not sign the user out
        if (request == null || request.currentPassword() == null
                || !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }
        if (request.newPassword() == null || request.newPassword().length() < 8) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be at least 8 characters"));
        }
        if (request.newPassword().equals(request.currentPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be different from the current one"));
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed"));
    }

    private ResponseEntity<?> invalidLogin() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
    }
}
