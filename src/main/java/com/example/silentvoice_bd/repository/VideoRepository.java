package com.example.silentvoice_bd.repository;



import com.example.silentvoice_bd.model.VideoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<VideoFile, UUID> {

    List<VideoFile> findByProcessingStatusOrderByUploadTimestampDesc(String processingStatus);

    @Query("SELECT v FROM VideoFile v ORDER BY v.uploadTimestamp DESC")
    List<VideoFile> findAllOrderByUploadTimestampDesc();

    List<VideoFile> findByContentTypeContainingIgnoreCase(String contentType);
}
