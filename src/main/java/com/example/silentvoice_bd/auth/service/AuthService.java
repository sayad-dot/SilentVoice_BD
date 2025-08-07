package com.example.silentvoice_bd.auth.service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.silentvoice_bd.auth.dto.AuthRequest;
import com.example.silentvoice_bd.auth.dto.AuthResponse;
import com.example.silentvoice_bd.auth.dto.OAuth2UserInfo;
import com.example.silentvoice_bd.auth.dto.RegisterRequest;
import com.example.silentvoice_bd.auth.model.Role;
import com.example.silentvoice_bd.auth.model.User;
import com.example.silentvoice_bd.auth.repository.RoleRepository;
import com.example.silentvoice_bd.auth.repository.UserRepository;
import com.example.silentvoice_bd.auth.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    // Traditional login
    public AuthResponse login(AuthRequest request) {
        logger.info("🔐 Traditional login attempt for: {}", request.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = (User) authentication.getPrincipal();
        String token = jwtTokenProvider.generateToken(user);

        logger.info("✅ Traditional login successful for: {}", request.getEmail());
        return new AuthResponse(token, user.getFullName());
    }

    // Traditional registration
    public AuthResponse register(RegisterRequest request) {
        logger.info("📝 Traditional registration attempt for: {}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName()
        );

        // Set default role
        Optional<Role> userRole = roleRepository.findByName("USER");
        if (userRole.isPresent()) {
            Set<Role> roles = new HashSet<>();
            roles.add(userRole.get());
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser);

        logger.info("✅ Traditional registration successful for: {}", request.getEmail());
        return new AuthResponse(token, savedUser.getFullName());
    }

    // NEW: Google OAuth Login
    public AuthResponse loginWithGoogle(OAuth2UserInfo googleUserInfo) {
        logger.info("🔐 Google login attempt for: {}", googleUserInfo.getEmail());

        // First try to find user by OAuth provider and ID
        Optional<User> existingOAuthUser = userRepository
                .findByOauthProviderAndOauthId("google", googleUserInfo.getGoogleId());

        if (existingOAuthUser.isPresent()) {
            User user = existingOAuthUser.get();
            // Update profile info in case it changed
            updateUserProfileFromGoogle(user, googleUserInfo);
            User updatedUser = userRepository.save(user);
            String token = jwtTokenProvider.generateToken(updatedUser);

            logger.info("✅ Google login successful (OAuth user): {}", googleUserInfo.getEmail());
            return new AuthResponse(token, updatedUser.getFullName());
        }

        // If no OAuth user, check if email exists as traditional user
        Optional<User> existingEmailUser = userRepository.findByEmail(googleUserInfo.getEmail());
        if (existingEmailUser.isPresent()) {
            User user = existingEmailUser.get();
            if ("local".equals(user.getOauthProvider())) {
                // Convert traditional user to Google OAuth user
                user.setOauthProvider("google");
                user.setOauthId(googleUserInfo.getGoogleId());
                user.setProfilePictureUrl(googleUserInfo.getProfilePicture());
                user.setEmailVerified(true);

                User updatedUser = userRepository.save(user);
                String token = jwtTokenProvider.generateToken(updatedUser);

                logger.info("✅ Google login successful (converted traditional user): {}", googleUserInfo.getEmail());
                return new AuthResponse(token, updatedUser.getFullName());
            }
        }

        // User doesn't exist
        logger.warn("❌ Google login failed - user not found: {}", googleUserInfo.getEmail());
        throw new IllegalArgumentException("Account not found. Please sign up with Google instead.");
    }

    // NEW: Google OAuth Registration
    public AuthResponse registerWithGoogle(OAuth2UserInfo googleUserInfo) {
        logger.info("📝 Google registration attempt for: {}", googleUserInfo.getEmail());

        // Check if user already exists with this Google ID
        if (userRepository.existsByOauthProviderAndOauthId("google", googleUserInfo.getGoogleId())) {
            logger.warn("❌ Google registration failed - Google ID already exists: {}", googleUserInfo.getGoogleId());
            throw new IllegalArgumentException("Google account already registered. Please sign in instead.");
        }

        // Check if email already exists (traditional or other OAuth)
        if (userRepository.existsByEmail(googleUserInfo.getEmail())) {
            logger.warn("❌ Google registration failed - email already exists: {}", googleUserInfo.getEmail());
            throw new IllegalArgumentException("Email already registered. Please sign in with Google instead.");
        }

        // Create new Google OAuth user
        User user = new User(
                googleUserInfo.getEmail(),
                googleUserInfo.getFullName(),
                "google",
                googleUserInfo.getGoogleId(),
                googleUserInfo.getProfilePicture()
        );

        // Set email as verified for Google users
        user.setEmailVerified(true);

        // Set default role
        Optional<Role> userRole = roleRepository.findByName("USER");
        if (userRole.isPresent()) {
            Set<Role> roles = new HashSet<>();
            roles.add(userRole.get());
            user.setRoles(roles);
        }

        User savedUser = userRepository.save(user);
        String token = jwtTokenProvider.generateToken(savedUser);

        logger.info("✅ Google registration successful for: {}", googleUserInfo.getEmail());
        return new AuthResponse(token, savedUser.getFullName());
    }

    // Helper method to update user profile from Google
    private void updateUserProfileFromGoogle(User user, OAuth2UserInfo googleUserInfo) {
        // Update profile picture if it's different
        if (googleUserInfo.getProfilePicture() != null
                && !googleUserInfo.getProfilePicture().equals(user.getProfilePictureUrl())) {
            user.setProfilePictureUrl(googleUserInfo.getProfilePicture());
        }

        // Update full name if it's different
        if (googleUserInfo.getFullName() != null
                && !googleUserInfo.getFullName().equals(user.getFullName())) {
            user.setFullName(googleUserInfo.getFullName());
        }

        // Ensure email is verified for Google users
        if (!user.getEmailVerified()) {
            user.setEmailVerified(true);
        }
    }
}
