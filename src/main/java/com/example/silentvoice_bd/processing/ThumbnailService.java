package com.example.silentvoice_bd.processing;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.silentvoice_bd.config.VideoProcessingConfiguration;
import com.example.silentvoice_bd.model.VideoFile;
//import java.util.UUID;

@Service
public class ThumbnailService {

    @Autowired
    private VideoProcessingConfiguration config;

    public String generateThumbnail(VideoFile videoFile) throws Exception {
        // Create thumbnail directory
        Path thumbnailDir = Paths.get(config.getThumbnailsOutputDir());
        Files.createDirectories(thumbnailDir);

        String thumbnailFileName = videoFile.getId().toString() + "_thumbnail.jpg";
        File thumbnailFile = thumbnailDir.resolve(thumbnailFileName).toFile();

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile.getFilePath())) {
            grabber.start();

            // Seek to 10% of video duration for thumbnail
            double duration = grabber.getLengthInTime() / 1000000.0; // Convert to seconds
            double thumbnailTime = Math.min(duration * 0.1, 5.0); // Max 5 seconds into video

            grabber.setTimestamp((long) (thumbnailTime * 1000000));
            Frame frame = grabber.grabImage();

            if (frame != null) {
                try (Java2DFrameConverter converter = new Java2DFrameConverter()) {
                    BufferedImage originalImage = converter.convert(frame);

                    if (originalImage != null) {
                        // Resize image to thumbnail size
                        BufferedImage thumbnailImage = resizeImage(
                            originalImage,
                            config.getThumbnailWidth(),
                            config.getThumbnailHeight()
                        );

                        ImageIO.write(thumbnailImage, "jpg", thumbnailFile);
                        return thumbnailFile.getAbsolutePath();
                    }
                }
            }

            grabber.stop();
        }

        throw new Exception("Could not generate thumbnail for video: " + videoFile.getOriginalFilename());
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        // Calculate new dimensions maintaining aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double aspectRatio = (double) originalWidth / originalHeight;
        int newWidth, newHeight;

        if (aspectRatio > (double) targetWidth / targetHeight) {
            newWidth = targetWidth;
            newHeight = (int) (targetWidth / aspectRatio);
        } else {
            newWidth = (int) (targetHeight * aspectRatio);
            newHeight = targetHeight;
        }

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resizedImage.createGraphics();

        // Enable high-quality rendering
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return resizedImage;
    }

    public void deleteThumbnail(String thumbnailPath) {
        if (thumbnailPath != null) {
            try {
                Files.deleteIfExists(Paths.get(thumbnailPath));
            } catch (java.io.IOException e) {
                System.err.println("Failed to delete thumbnail: " + thumbnailPath);
            }
        }
    }
}
