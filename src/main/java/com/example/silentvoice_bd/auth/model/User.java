package com.example.silentvoice_bd.auth.model;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(length = 60) // Nullable for OAuth users
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private Boolean isVerified = true;

    // ===== OAUTH FIELDS =====
    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider = "local"; // 'google', 'local', etc.

    @Column(name = "oauth_id", length = 255)
    private String oauthId; // Google user ID

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl; // Google profile picture URL

    @Column(name = "email_verified")
    private Boolean emailVerified = false; // Track email verification status

    // ===== ADMIN MANAGEMENT FIELDS =====
    @Enumerated(EnumType.STRING)
    @Column(name = "user_status")
    private UserStatus status = UserStatus.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles_enum", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    private Set<UserRole> roles = new HashSet<>();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "login_count")
    private Integer loginCount = 0;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // ===== ENUMS =====
    public enum UserStatus {
        ACTIVE, SUSPENDED, BANNED, DELETED
    }

    public enum UserRole {
        USER, ADMIN, MODERATOR
    }

    // ===== CONSTRUCTORS =====
    // Constructor for traditional users
    public User(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.oauthProvider = "local";
        this.isVerified = true;
        this.emailVerified = false;
        this.status = UserStatus.ACTIVE;
        this.roles = new HashSet<>(Arrays.asList(UserRole.USER));
        this.loginCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Constructor for Google OAuth users
    public User(String email, String fullName, String oauthProvider, String oauthId, String profilePictureUrl) {
        this.email = email;
        this.fullName = fullName;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.profilePictureUrl = profilePictureUrl;
        this.emailVerified = true; // Google emails are pre-verified
        this.isVerified = true;
        this.status = UserStatus.ACTIVE;
        this.roles = new HashSet<>(Arrays.asList(UserRole.USER));
        this.loginCount = 0;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ===== HELPER METHODS =====
    public boolean isOAuthUser() {
        return !"local".equals(oauthProvider);
    }

    public boolean isGoogleUser() {
        return "google".equals(oauthProvider);
    }

    public boolean hasProfilePicture() {
        return profilePictureUrl != null && !profilePictureUrl.trim().isEmpty();
    }

    public boolean hasRole(UserRole role) {
        return this.roles != null && this.roles.contains(role);
    }

    public void addRole(UserRole role) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(role);
        this.updatedAt = Instant.now();
    }

    public void removeRole(UserRole role) {
        if (this.roles != null) {
            this.roles.remove(role);
            this.updatedAt = Instant.now();
        }
    }

    // 🎯 NEW: Method to assign admin role
    public void makeAdmin() {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(UserRole.ADMIN);
        this.updatedAt = Instant.now();
    }

    public void updateLoginInfo() {
        this.lastLoginAt = Instant.now();
        this.loginCount = (this.loginCount == null ? 0 : this.loginCount) + 1;
    }

    // ===== LIFECYCLE CALLBACKS =====
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ===== USERDETAILS IMPLEMENTATION =====
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();
        if (this.roles != null) {
            for (UserRole role : this.roles) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
            }
        }
        // Default to USER role if no roles assigned
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.status != UserStatus.DELETED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status != UserStatus.BANNED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.isVerified && this.status == UserStatus.ACTIVE;
    }

    // ===== ADMIN MANAGEMENT METHODS =====
    public void updateStatus(UserStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    public void setRoles(Set<UserRole> newRoles) {
        this.roles = newRoles != null ? new HashSet<>(newRoles) : new HashSet<>(Arrays.asList(UserRole.USER));
        this.updatedAt = Instant.now();
    }

    // Convert roles to string array for JSON serialization
    public List<String> getRoleNames() {
        if (this.roles == null || this.roles.isEmpty()) {
            return Arrays.asList("USER");
        }
        return this.roles.stream()
                .map(UserRole::name)
                .sorted()
                .toList();
    }
}
