package com.example.silentvoice_bd.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.silentvoice_bd.admin.model.AdminAuditLog;
import com.example.silentvoice_bd.admin.model.FeatureFlag;
import com.example.silentvoice_bd.admin.model.SupportTicket;
import com.example.silentvoice_bd.admin.service.AdminService;
import com.example.silentvoice_bd.auth.model.User;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService service;

    @GetMapping("/audit-logs")
    public ResponseEntity<List<AdminAuditLog>> getAuditLogs() {
        return ResponseEntity.ok(service.listAuditLogs());
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<SupportTicket>> getTickets() {
        return ResponseEntity.ok(service.listTickets());
    }

    @PatchMapping("/tickets/{id}")
    public ResponseEntity<SupportTicket> updateTicket(
            @PathVariable Long id,
            @RequestParam String status,
            @AuthenticationPrincipal User admin) {

        SupportTicket updated = service.updateTicketStatus(id, status);
        service.recordAudit(admin.getId(), "UPDATE_TICKET", "SupportTicket", id);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/feature-flags")
    public ResponseEntity<List<FeatureFlag>> getFlags() {
        return ResponseEntity.ok(service.listFlags());
    }

    @PatchMapping("/feature-flags/{id}")
    public ResponseEntity<FeatureFlag> toggleFlag(
            @PathVariable Long id,
            @RequestParam boolean enabled,
            @AuthenticationPrincipal User admin) {

        FeatureFlag f = service.toggleFeature(id, enabled);
        service.recordAudit(admin.getId(), "TOGGLE_FLAG", "FeatureFlag", id);
        return ResponseEntity.ok(f);
    }
}
