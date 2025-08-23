package com.example.silentvoice_bd.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.silentvoice_bd.admin.model.AdminAuditLog;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
}
