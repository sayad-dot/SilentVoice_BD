package com.example.silentvoice_bd.auth.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.silentvoice_bd.auth.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Find user by OAuth provider and OAuth ID
    Optional<User> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    // Check if OAuth ID exists for a provider
    boolean existsByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    // Find all users by OAuth provider
    List<User> findByOauthProvider(String oauthProvider);
}
