package com.pradyumnan.bpm_service.security;

import com.pradyumnan.bpm_service.model.AppUser;
import com.pradyumnan.bpm_service.model.UserStatus;
import com.pradyumnan.bpm_service.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

    // A user who must change their password gets only this authority, which is
    // enough to call /auth/change-password and nothing else.
    public static final String PASSWORD_CHANGE_AUTHORITY = "ROLE_PASSWORD_CHANGE_ONLY";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            String where = request.getMethod() + " " + request.getRequestURI();

            if (!jwtService.isValid(token)) {
                logger.info("Unauthenticated: {} carried an invalid or expired token", where);
            } else {
                String username = jwtService.extractUsername(token);

                // The database, not the token, is the source of truth for role and status.
                // Disabling a user or changing a role therefore takes effect on the next request.
                Optional<AppUser> found = userRepository.findByUsername(username);
                if (found.isEmpty()) {
                    logger.info("Unauthenticated: {} for '{}', who no longer exists", where, username);
                } else if (found.get().getStatus() != UserStatus.ACTIVE) {
                    logger.info("Unauthenticated: {} for '{}', whose account is {}",
                            where, username, found.get().getStatus());
                } else {
                    AppUser u = found.get();
                    String authority = u.isMustChangePassword()
                            ? PASSWORD_CHANGE_AUTHORITY
                            : "ROLE_" + u.getRole().name();
                    var auth = new UsernamePasswordAuthenticationToken(
                            u.getUsername(), null, List.of(new SimpleGrantedAuthority(authority)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }
}
