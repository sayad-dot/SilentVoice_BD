package com.example.silentvoice_bd.processing;

import com.example.silentvoice_bd.config.VideoProcessingConfiguration;
import com.example.silentvoice_bd.model.ExtractedFrame;
import com.example.silentvoice_bd.model.VideoFile;
import com.example.silentvoice_bd.model.VideoMetadata;
import com.example.silentvoice_bd.repository.ExtractedFrameRepository;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FrameExtractionService {

    @Autowired
    private ExtractedFrameRepository extractedFrameRepository;

    @Autowired
    private VideoProcessingConfiguration config;

    public List<ExtractedFrame> extractFrames(VideoFile videoFile, VideoMetadata metadata) throws Exception {
        List<ExtractedFrame> extractedFrames = new ArrayList<>();

        // Create output directory
        Path outputDir = Paths.get(config.getFramesOutputDir(), videoFile.getId().toString());
        Files.createDirectories(outputDir);

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile.getFilePath())) {
            grabber.start();

            Java2DFrameConverter converter = new Java2DFrameConverter();
            double frameRate = grabber.getFrameRate();
            double totalDuration = grabber.getLengthInTime() / 1000000.0; // Convert to seconds

            // Calculate frame extraction interval
            double interval = config.getFrameExtractionInterval();
            int maxFrames = config.getMaxFramesPerVideo();

            if (totalDuration / interval > maxFrames) {
                interval = totalDuration / maxFrames;
            }

            int frameNumber = 0;
            double currentTime = 0;

            while (currentTime < totalDuration && extractedFrames.size() < maxFrames) {
                // Seek to the desired timestamp
                grabber.setTimestamp((long) (currentTime * 1000000));
                Frame frame = grabber.grabImage();

                if (frame != null) {
                    BufferedImage bufferedImage = converter.convert(frame);

                    if (bufferedImage != null) {
                        // Save frame as image
                        String filename = String.format("frame_%06d_%.3fs.jpg", frameNumber, currentTime);
                        File frameFile = outputDir.resolve(filename).toFile();
                        ImageIO.write(bufferedImage, "jpg", frameFile);

                        // Create database record
                        ExtractedFrame extractedFrame = new ExtractedFrame(
                            videoFile.getId(),
                            frameNumber,
                            BigDecimal.valueOf(currentTime),
                            frameFile.getAbsolutePath()
                        );
                        extractedFrame.setWidth(bufferedImage.getWidth());
                        extractedFrame.setHeight(bufferedImage.getHeight());

                        // Determine if this is a keyframe (simplified logic)
                        extractedFrame.setIsKeyframe(frameNumber % 30 == 0); // Every 30th frame

                        extractedFrames.add(extractedFrame);
                        frameNumber++;
                    }
                }

                currentTime += interval;
            }

            grabber.stop();
        }

        // Save all extracted frames to database
        return extractedFrameRepository.saveAll(extractedFrames);
    }

    public List<ExtractedFrame> getFramesByVideoId(UUID videoFileId) {
        return extractedFrameRepository.findByVideoFileIdOrderByTimestampSeconds(videoFileId);
    }

    public List<ExtractedFrame> getKeyFramesByVideoId(UUID videoFileId) {
        return extractedFrameRepository.findByVideoFileIdAndIsKeyframeTrue(videoFileId);
    }

    public void deleteFrames(UUID videoFileId) {
        List<ExtractedFrame> frames = extractedFrameRepository.findByVideoFileIdOrderByTimestampSeconds(videoFileId);

        // Delete physical files
        for (ExtractedFrame frame : frames) {
            try {
                Files.deleteIfExists(Paths.get(frame.getFilePath()));
            } catch (IOException e) {
                // Log error but continue
                System.err.println("Failed to delete frame file: " + frame.getFilePath());
            }
        }

        // Delete database records
        extractedFrameRepository.deleteByVideoFileId(videoFileId);

        // Delete frame directory
        try {
            Path frameDir = Paths.get(config.getFramesOutputDir(), videoFileId.toString());
            Files.deleteIfExists(frameDir);
        } catch (IOException e) {
            System.err.println("Failed to delete frame directory for video: " + videoFileId);
        }
    }
}
