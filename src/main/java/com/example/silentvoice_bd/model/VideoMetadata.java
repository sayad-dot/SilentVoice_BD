package com.example.silentvoice_bd.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "video_metadata")
public class VideoMetadata {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "video_file_id", nullable = false)
    private UUID videoFileId;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "frame_rate", precision = 5, scale = 2)
    private BigDecimal frameRate;

    @Column
    private Integer width;

    @Column
    private Integer height;

    @Column
    private Integer bitrate;

    @Column(name = "video_codec", length = 50)
    private String videoCodec;

    @Column(name = "audio_codec", length = 50)
    private String audioCodec;

    @Column(name = "file_format", length = 20)
    private String fileFormat;

    @Column(name = "has_audio")
    private Boolean hasAudio = false;

    @Column(name = "thumbnail_path", length = 500)
    private String thumbnailPath;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Constructors
    public VideoMetadata() {}

    public VideoMetadata(UUID videoFileId) {
        this.videoFileId = videoFileId;
    }

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getVideoFileId() { return videoFileId; }
    public void setVideoFileId(UUID videoFileId) { this.videoFileId = videoFileId; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public BigDecimal getFrameRate() { return frameRate; }
    public void setFrameRate(BigDecimal frameRate) { this.frameRate = frameRate; }

    public Integer getWidth() { return width; }
    public void setWidth(Integer width) { this.width = width; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Integer getBitrate() { return bitrate; }
    public void setBitrate(Integer bitrate) { this.bitrate = bitrate; }

    public String getVideoCodec() { return videoCodec; }
    public void setVideoCodec(String videoCodec) { this.videoCodec = videoCodec; }

    public String getAudioCodec() { return audioCodec; }
    public void setAudioCodec(String audioCodec) { this.audioCodec = audioCodec; }

    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }

    public Boolean getHasAudio() { return hasAudio; }
    public void setHasAudio(Boolean hasAudio) { this.hasAudio = hasAudio; }

    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
