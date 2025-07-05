package com.example.silentvoice_bd.processing;

import java.io.File;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.silentvoice_bd.model.VideoFile;
import com.example.silentvoice_bd.model.VideoMetadata;
import com.example.silentvoice_bd.repository.VideoMetadataRepository;

import ws.schild.jave.MultimediaObject;
import ws.schild.jave.info.MultimediaInfo;

@Service
public class VideoMetadataService {

    @Autowired
    private VideoMetadataRepository videoMetadataRepository;

    public VideoMetadata extractMetadata(VideoFile videoFile) throws Exception {
        File file = new File(videoFile.getFilePath());
        MultimediaObject multimediaObject = new MultimediaObject(file);
        MultimediaInfo info = multimediaObject.getInfo();

        VideoMetadata metadata = new VideoMetadata(videoFile.getId());

        // Basic video information
        if (info.getDuration() > 0) {
            metadata.setDurationSeconds((int) (info.getDuration() / 1000));
        }

        if (info.getVideo() != null) {
            metadata.setWidth(info.getVideo().getSize().getWidth());
            metadata.setHeight(info.getVideo().getSize().getHeight());
            metadata.setBitrate(info.getVideo().getBitRate());
            metadata.setVideoCodec(info.getVideo().getDecoder());

            if (info.getVideo().getFrameRate() > 0) {
                metadata.setFrameRate(BigDecimal.valueOf(info.getVideo().getFrameRate()));
            }
        }

        if (info.getAudio() != null) {
            metadata.setHasAudio(true);
            metadata.setAudioCodec(info.getAudio().getDecoder());
        }

        metadata.setFileFormat(info.getFormat());

        return videoMetadataRepository.save(metadata);
    }

    public Optional<VideoMetadata> getMetadataByVideoId(UUID videoFileId) {
        return videoMetadataRepository.findByVideoFileId(videoFileId);
    }

    public void deleteMetadata(UUID videoFileId) {
        videoMetadataRepository.deleteByVideoFileId(videoFileId);
    }
}
