package com.example.silentvoice_bd.admin.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.silentvoice_bd.admin.model.AdminAuditLog;
import com.example.silentvoice_bd.admin.model.FeatureFlag;
import com.example.silentvoice_bd.admin.model.SupportTicket;
import com.example.silentvoice_bd.admin.repository.AdminAuditLogRepository;
import com.example.silentvoice_bd.admin.repository.FeatureFlagRepository;
import com.example.silentvoice_bd.admin.repository.SupportTicketRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminAuditLogRepository auditRepo;
    private final SupportTicketRepository ticketRepo;
    private final FeatureFlagRepository flagRepo;

    public List<AdminAuditLog> listAuditLogs() {
        return auditRepo.findAll();
    }

    /**
     * Record an admin action.
     *
     * @param adminId the UUID of the admin user
     * @param action action name
     * @param entity entity name
     * @param targetId database ID of the target entity (Long)
     */
    public void recordAudit(UUID adminId, String action, String entity, Long targetId) {
        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(adminId);
        log.setAction(action);
        log.setTargetEntity(entity);
        // store Long as String in AdminAuditLog.targetId; see below
        log.setTargetId(targetId.toString());
        log.setTimestamp(Instant.now());
        auditRepo.save(log);
    }

    public List<SupportTicket> listTickets() {
        return ticketRepo.findAll();
    }

    public SupportTicket updateTicketStatus(Long id, String status) {
        SupportTicket t = ticketRepo.findById(id).orElseThrow();
        t.setStatus(status);
        t.setUpdatedAt(Instant.now());
        return ticketRepo.save(t);
    }

    public List<FeatureFlag> listFlags() {
        return flagRepo.findAll();
    }

    public FeatureFlag toggleFeature(Long id, boolean enabled) {
        FeatureFlag f = flagRepo.findById(id).orElseThrow();
        f.setEnabled(enabled);
        return flagRepo.save(f);
    }
}
