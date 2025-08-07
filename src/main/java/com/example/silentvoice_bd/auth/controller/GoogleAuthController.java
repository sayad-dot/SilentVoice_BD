package com.example.silentvoice_bd.auth.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.silentvoice_bd.auth.dto.AuthResponse;
import com.example.silentvoice_bd.auth.dto.OAuth2UserInfo;
import com.example.silentvoice_bd.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/google")
@CrossOrigin(origins = "http://localhost:3000")  // Added for frontend communication
@RequiredArgsConstructor
public class GoogleAuthController {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAuthController.class);
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody OAuth2UserInfo googleUserInfo) {
        try {
            logger.info("🔐 Google login attempt for email: {}", googleUserInfo.getEmail());

            // Replace hasCompleteProfile() with direct validation
            if (googleUserInfo.getGoogleId() == null || googleUserInfo.getEmail() == null) {
                logger.warn("❌ Incomplete Google user profile for: {}", googleUserInfo.getEmail());
                return ResponseEntity.badRequest()
                        .body("Incomplete user profile from Google");
            }

            AuthResponse response = authService.loginWithGoogle(googleUserInfo);
            logger.info("✅ Google login successful for: {}", googleUserInfo.getEmail());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("❌ Google login failed - user not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Account not found. Please sign up with Google instead.");
        } catch (Exception e) {
            logger.error("💥 Google login error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Google login failed: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> googleRegister(@Valid @RequestBody OAuth2UserInfo googleUserInfo) {
        try {
            logger.info("🔐 Google registration attempt for email: {}", googleUserInfo.getEmail());

            // Replace hasCompleteProfile() with direct validation
            if (googleUserInfo.getGoogleId() == null || googleUserInfo.getEmail() == null
                    || googleUserInfo.getFullName() == null) {
                logger.warn("❌ Incomplete Google user profile for: {}", googleUserInfo.getEmail());
                return ResponseEntity.badRequest()
                        .body("Incomplete user profile from Google");
            }

            AuthResponse response = authService.registerWithGoogle(googleUserInfo);
            logger.info("✅ Google registration successful for: {}", googleUserInfo.getEmail());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("❌ Google registration failed - email exists: {}", e.getMessage());
            if (e.getMessage().contains("already registered")) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body("Email already registered. Please sign in with Google instead.");
            }
            return ResponseEntity.badRequest()
                    .body("Registration failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("💥 Google registration error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Google registration failed: " + e.getMessage());
        }
    }
}
