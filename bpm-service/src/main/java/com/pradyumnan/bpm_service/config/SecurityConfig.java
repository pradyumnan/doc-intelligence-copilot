package com.pradyumnan.bpm_service.config;

import com.pradyumnan.bpm_service.repository.UserRepository;
import com.pradyumnan.bpm_service.security.JwtAuthFilter;
import com.pradyumnan.bpm_service.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public SecurityConfig(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                // public
                .requestMatchers("/health", "/auth/register", "/auth/login").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // any signed-in user, including one who must still change their password
                .requestMatchers("/auth/change-password", "/auth/me").authenticated()
                // administrators only
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // working with documents: reviewers and admins
                .requestMatchers(HttpMethod.POST, "/cases/**").hasAnyRole("ADMIN", "REVIEWER")
                // reading cases: every role
                .requestMatchers(HttpMethod.GET, "/cases/**").hasAnyRole("ADMIN", "REVIEWER", "VIEWER")
                // anything else needs a real role
                .anyRequest().hasAnyRole("ADMIN", "REVIEWER", "VIEWER")
            )
            .addFilterBefore(new JwtAuthFilter(jwtService, userRepository), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
