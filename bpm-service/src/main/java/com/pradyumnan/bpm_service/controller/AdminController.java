package com.pradyumnan.bpm_service.controller;

import com.pradyumnan.bpm_service.model.AppUser;
import com.pradyumnan.bpm_service.model.Role;
import com.pradyumnan.bpm_service.model.UserStatus;
import com.pradyumnan.bpm_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@RestController
@RequestMapping("/admin/users")
public class AdminController {

    public record UserView(Long id, String username, Role role, UserStatus status, LocalDateTime createdAt) {}

    public record RoleRequest(Role role) {}

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    // No look-alike characters (0/O, 1/l/I) so a temporary password is easy to read out
    private static final String TEMP_PASSWORD_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UserView> list() {
        return userRepository.findAll().stream()
                .sorted(Comparator.comparing(AppUser::getId))
                .map(AdminController::toView)
                .toList();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id, Authentication auth,
                                     @RequestBody(required = false) RoleRequest request) {
        Role role = (request != null && request.role() != null) ? request.role() : Role.REVIEWER;
        return apply(id, auth, false, user -> {
            user.setStatus(UserStatus.ACTIVE);
            user.setRole(role);
        }, "approved");
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<?> disable(@PathVariable Long id, Authentication auth) {
        return apply(id, auth, true, user -> user.setStatus(UserStatus.DISABLED), "disabled");
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<?> enable(@PathVariable Long id, Authentication auth) {
        return apply(id, auth, false, user -> user.setStatus(UserStatus.ACTIVE), "enabled");
    }

    @PostMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, Authentication auth, @RequestBody RoleRequest request) {
        if (request == null || request.role() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "A role is required"));
        }
        Role role = request.role();
        return apply(id, auth, true, user -> user.setRole(role), "role changed to " + role);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Long id, Authentication auth) {
        Optional<AppUser> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return notFound();
        }
        AppUser user = found.get();

        String temporary = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(temporary));
        user.setMustChangePassword(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // The password itself is never logged
        logger.info("Admin '{}' reset the password of user '{}'", auth.getName(), user.getUsername());
        // Shown to the admin once; the user must change it at the next sign-in
        return ResponseEntity.ok(Map.of("username", user.getUsername(), "temporaryPassword", temporary));
    }

    private ResponseEntity<?> apply(Long id, Authentication auth, boolean protectSelf,
                                    Consumer<AppUser> change, String description) {
        Optional<AppUser> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return notFound();
        }
        AppUser user = found.get();
        // Stops an admin from locking themselves out by disabling or demoting their own account
        if (protectSelf && user.getUsername().equals(auth.getName())) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot do this to your own account"));
        }
        change.accept(user);
        userRepository.save(user);
        logger.info("Admin '{}' {} user '{}'", auth.getName(), description, user.getUsername());
        return ResponseEntity.ok(toView(user));
    }

    private static ResponseEntity<?> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
    }

    private static UserView toView(AppUser u) {
        return new UserView(u.getId(), u.getUsername(), u.getRole(), u.getStatus(), u.getCreatedAt());
    }

    private static String generateTemporaryPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(TEMP_PASSWORD_ALPHABET.charAt(RANDOM.nextInt(TEMP_PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }
}
