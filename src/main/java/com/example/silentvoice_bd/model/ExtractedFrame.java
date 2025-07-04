package com.example.silentvoice_bd.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "extracted_frames")
public class ExtractedFrame {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "video_file_id", nullable = false)
    private UUID videoFileId;

    @Column(name = "frame_number", nullable = false)
    private Integer frameNumber;

    @Column(name = "timestamp_seconds", nullable = false, precision = 8, scale = 3)
    private BigDecimal timestampSeconds;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column(name = "is_keyframe")
    private Boolean isKeyframe = false;

    @Column(name = "motion_score", precision = 5, scale = 3)
    private BigDecimal motionScore;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public ExtractedFrame() {}

    public ExtractedFrame(UUID videoFileId, Integer frameNumber, BigDecimal timestampSeconds, String filePath) {
        this.videoFileId = videoFileId;
        this.frameNumber = frameNumber;
        this.timestampSeconds = timestampSeconds;
        this.filePath = filePath;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getVideoFileId() { return videoFileId; }
    public void setVideoFileId(UUID videoFileId) { this.videoFileId = videoFileId; }

    public Integer getFrameNumber() { return frameNumber; }
    public void setFrameNumber(Integer frameNumber) { this.frameNumber = frameNumber; }

    public BigDecimal getTimestampSeconds() { return timestampSeconds; }
    public void setTimestampSeconds(BigDecimal timestampSeconds) { this.timestampSeconds = timestampSeconds; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Boolean getIsKeyframe() { return isKeyframe; }
    public void setIsKeyframe(Boolean isKeyframe) { this.isKeyframe = isKeyframe; }

    public BigDecimal getMotionScore() { return motionScore; }
    public void setMotionScore(BigDecimal motionScore) { this.motionScore = motionScore; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
