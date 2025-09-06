package com.example.silentvoice_bd.learning.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.silentvoice_bd.learning.model.Sign;

@Repository
public interface SignRepository extends JpaRepository<Sign, Long> {
    // Add custom query methods if needed
}
