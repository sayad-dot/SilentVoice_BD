package com.example.silentvoice_bd.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.silentvoice_bd.admin.model.SupportTicket;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> { }
