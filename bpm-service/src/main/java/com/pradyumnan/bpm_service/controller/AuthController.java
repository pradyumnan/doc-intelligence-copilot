package com.pradyumnan.bpm_service.controller;

import com.pradyumnan.bpm_service.model.AppUser;
import com.pradyumnan.bpm_service.repository.UserRepository;
import com.pradyumnan.bpm_service.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public record Credentials(String username, String password) {}

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
        if (creds.username() == null || creds.username().isBlank()
                || creds.password() == null || creds.password().length() < 8) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username required and password must be at least 8 characters"));
        }
        if (userRepository.findByUsername(creds.username()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username already taken"));
        }
        AppUser user = new AppUser();
        user.setUsername(creds.username());
        user.setPasswordHash(passwordEncoder.encode(creds.password()));
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credentials creds) {
        return userRepository.findByUsername(creds.username())
                .filter(u -> passwordEncoder.matches(creds.password(), u.getPasswordHash()))
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(Map.of("token", jwtService.generateToken(u.getUsername()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password")));
    }
}