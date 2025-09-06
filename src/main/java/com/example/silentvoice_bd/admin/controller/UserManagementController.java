package com.example.silentvoice_bd.admin.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.silentvoice_bd.admin.service.AdminService;
import com.example.silentvoice_bd.auth.model.User;
import com.example.silentvoice_bd.auth.service.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserService userService;
    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }

    @PatchMapping("/{userId}/role")
    public ResponseEntity<User> updateUserRole(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {

        String newRole = request.get("role");
        User updatedUser = userService.updateUserRole(userId, newRole);

        // Record audit log
        adminService.recordAudit(admin.getId(), "UPDATE_USER_ROLE",
                "User", Long.valueOf(userId.toString().hashCode()));

        return ResponseEntity.ok(updatedUser);
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<User> updateUserStatus(
            @PathVariable UUID userId,
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal User admin) {

        String status = request.get("status"); // ACTIVE, SUSPENDED, BANNED
        User updatedUser = userService.updateUserStatus(userId, status);

        adminService.recordAudit(admin.getId(), "UPDATE_USER_STATUS",
                "User", Long.valueOf(userId.toString().hashCode()));

        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal User admin) {

        userService.deleteUser(userId);
        adminService.recordAudit(admin.getId(), "DELETE_USER",
                "User", Long.valueOf(userId.toString().hashCode()));

        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getUserStats() {
        Map<String, Object> stats = userService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }
}
