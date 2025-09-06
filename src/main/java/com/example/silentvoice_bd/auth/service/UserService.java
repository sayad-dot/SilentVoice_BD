package com.example.silentvoice_bd.auth.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.silentvoice_bd.auth.model.User;
import com.example.silentvoice_bd.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User updateUserRole(UUID userId, String roles) {
        User user = getUserById(userId);

        // Parse roles string to enum set
        Set<User.UserRole> roleSet = new HashSet<>();
        if (roles != null && !roles.trim().isEmpty()) {
            String[] roleArray = roles.split(",");
            for (String role : roleArray) {
                try {
                    roleSet.add(User.UserRole.valueOf(role.trim().toUpperCase()));
                } catch (IllegalArgumentException e) {
                    // Skip invalid roles
                }
            }
        }

        // Ensure at least USER role
        if (roleSet.isEmpty()) {
            roleSet.add(User.UserRole.USER);
        }

        user.setRoles(roleSet);
        return userRepository.save(user);
    }

    public User updateUserStatus(UUID userId, String status) {
        User user = getUserById(userId);

        try {
            User.UserStatus userStatus = User.UserStatus.valueOf(status.toUpperCase());
            user.updateStatus(userStatus);
            return userRepository.save(user);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status: " + status);
        }
    }

    public void deleteUser(UUID userId) {
        User user = getUserById(userId);
        user.updateStatus(User.UserStatus.DELETED);
        userRepository.save(user);
    }

    public Map<String, Object> getUserStatistics() {
        List<User> allUsers = userRepository.findAll();

        Map<String, Object> stats = new HashMap<>();

        // Total users (excluding deleted)
        long totalUsers = allUsers.stream()
                .filter(u -> u.getStatus() != User.UserStatus.DELETED)
                .count();

        // Active users
        long activeUsers = allUsers.stream()
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .count();

        // New users this month
        Instant oneMonthAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        long newUsersThisMonth = allUsers.stream()
                .filter(u -> u.getCreatedAt() != null && u.getCreatedAt().isAfter(oneMonthAgo))
                .count();

        // Suspended/Banned users
        long suspendedUsers = allUsers.stream()
                .filter(u -> u.getStatus() == User.UserStatus.SUSPENDED
                || u.getStatus() == User.UserStatus.BANNED)
                .count();

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("newUsersThisMonth", newUsersThisMonth);
        stats.put("suspendedUsers", suspendedUsers);

        return stats;
    }
}
