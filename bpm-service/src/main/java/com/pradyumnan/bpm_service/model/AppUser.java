package com.pradyumnan.bpm_service.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role = Role.VIEWER;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.PENDING;

    // Wrapper types so older rows with NULL in these new columns still load
    private Boolean mustChangePassword = false;
    private Integer failedLoginAttempts = 0;
    private LocalDateTime lockedUntil;
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    // A missing value is treated as the safest default
    public Role getRole() { return role == null ? Role.VIEWER : role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status == null ? UserStatus.PENDING : status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public boolean isMustChangePassword() { return Boolean.TRUE.equals(mustChangePassword); }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public int getFailedLoginAttempts() { return failedLoginAttempts == null ? 0 : failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
