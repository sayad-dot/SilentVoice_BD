package com.example.silentvoice_bd.repository;

import com.example.silentvoice_bd.model.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, UUID> {

    Optional<VideoMetadata> findByVideoFileId(UUID videoFileId);

    void deleteByVideoFileId(UUID videoFileId);
}
