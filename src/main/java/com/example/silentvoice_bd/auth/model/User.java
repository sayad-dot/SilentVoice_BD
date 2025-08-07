// src/main/java/com/example/silentvoice_bd/auth/model/User.java - UPDATED VERSION
package com.example.silentvoice_bd.auth.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
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

    @Column(length = 60) // Made nullable for OAuth users
    private String password;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false)
    private Boolean isVerified = true;

    // NEW: OAuth provider fields
    @Column(name = "oauth_provider", length = 50)
    private String oauthProvider = "local"; // 'google', 'local', etc.

    @Column(name = "oauth_id", length = 255)
    private String oauthId; // Google user ID

    @Column(name = "profile_picture_url", length = 500)
    private String profilePictureUrl; // Google profile picture URL

    @Column(name = "email_verified")
    private Boolean emailVerified = false; // Track email verification status

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    // Constructor for traditional users
    public User(String email, String password, String fullName) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.oauthProvider = "local";
        this.isVerified = true;
        this.emailVerified = false; // Can be set to true after email verification
    }

    // NEW: Constructor for Google OAuth users
    public User(String email, String fullName, String oauthProvider, String oauthId, String profilePictureUrl) {
        this.email = email;
        this.fullName = fullName;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.profilePictureUrl = profilePictureUrl;
        this.emailVerified = true; // Google emails are pre-verified
        this.isVerified = true;
        // No password for OAuth users
    }

    // NEW: Helper methods for OAuth
    public boolean isOAuthUser() {
        return !"local".equals(oauthProvider);
    }

    public boolean isGoogleUser() {
        return "google".equals(oauthProvider);
    }

    public boolean hasProfilePicture() {
        return profilePictureUrl != null && !profilePictureUrl.trim().isEmpty();
    }

    // UserDetails implementation
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getName()))
                .toList();
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        // OAuth users don't have passwords
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isVerified;
    }
}
