package com.example.silentvoice_bd.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "video.processing")
public class VideoProcessingConfiguration {

    private String framesOutputDir = "./uploads/frames/";
    private String thumbnailsOutputDir = "./uploads/thumbnails/";
    private int maxFramesPerVideo = 1000;
    private int thumbnailWidth = 320;
    private int thumbnailHeight = 240;
    private double frameExtractionInterval = 1.0; // seconds

    // Getters and setters
    public String getFramesOutputDir() { return framesOutputDir; }
    public void setFramesOutputDir(String framesOutputDir) { this.framesOutputDir = framesOutputDir; }

    public String getThumbnailsOutputDir() { return thumbnailsOutputDir; }
    public void setThumbnailsOutputDir(String thumbnailsOutputDir) { this.thumbnailsOutputDir = thumbnailsOutputDir; }

    public int getMaxFramesPerVideo() { return maxFramesPerVideo; }
    public void setMaxFramesPerVideo(int maxFramesPerVideo) { this.maxFramesPerVideo = maxFramesPerVideo; }

    public int getThumbnailWidth() { return thumbnailWidth; }
    public void setThumbnailWidth(int thumbnailWidth) { this.thumbnailWidth = thumbnailWidth; }

    public int getThumbnailHeight() { return thumbnailHeight; }
    public void setThumbnailHeight(int thumbnailHeight) { this.thumbnailHeight = thumbnailHeight; }

    public double getFrameExtractionInterval() { return frameExtractionInterval; }
    public void setFrameExtractionInterval(double frameExtractionInterval) { this.frameExtractionInterval = frameExtractionInterval; }
}
