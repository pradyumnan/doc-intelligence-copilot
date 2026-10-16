package com.pradyumnan.bpm_service.security;

import com.pradyumnan.bpm_service.model.UserStatus;
import com.pradyumnan.bpm_service.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

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
            if (jwtService.isValid(token)) {
                String username = jwtService.extractUsername(token);

                // The database, not the token, is the source of truth for role and status.
                // Disabling a user or changing a role therefore takes effect on the next request.
                userRepository.findByUsername(username)
                        .filter(u -> u.getStatus() == UserStatus.ACTIVE)
                        .ifPresent(u -> {
                            String authority = u.isMustChangePassword()
                                    ? PASSWORD_CHANGE_AUTHORITY
                                    : "ROLE_" + u.getRole().name();
                            var auth = new UsernamePasswordAuthenticationToken(
                                    u.getUsername(), null, List.of(new SimpleGrantedAuthority(authority)));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            }
        }
        chain.doFilter(request, response);
    }
}
