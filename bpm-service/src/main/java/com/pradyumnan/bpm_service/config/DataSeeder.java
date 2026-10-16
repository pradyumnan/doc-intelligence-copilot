package com.pradyumnan.bpm_service.config;

import com.pradyumnan.bpm_service.model.AppUser;
import com.pradyumnan.bpm_service.model.Role;
import com.pradyumnan.bpm_service.model.UserStatus;
import com.pradyumnan.bpm_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator and an optional demo reviewer on startup.
 * Credentials come from configuration (properties or environment variables such as
 * APP_ADMIN_USERNAME / APP_ADMIN_PASSWORD), never from code. An account that already
 * exists is left untouched, so restarting is safe.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String demoUsername;
    private final String demoPassword;

    public DataSeeder(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${app.admin.username:}") String adminUsername,
                      @Value("${app.admin.password:}") String adminPassword,
                      @Value("${app.demo.username:}") String demoUsername,
                      @Value("${app.demo.password:}") String demoPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.demoUsername = demoUsername;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        seed(adminUsername, adminPassword, Role.ADMIN);
        seed(demoUsername, demoPassword, Role.REVIEWER);
    }

    private void seed(String username, String password, Role role) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return;
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        logger.info("Seeded {} account '{}'", role, username);
    }
}
