package com.example.silentvoice_bd.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final String DATASET_PATH = "dataset/bdslw60/archive";

    @GetMapping("/video/{signName}/{fileName}")
    public ResponseEntity<Resource> getVideo(
            @PathVariable String signName,
            @PathVariable String fileName) throws IOException {

        try {
            Path videoPath = Paths.get(DATASET_PATH)
                    .resolve(signName)
                    .resolve(fileName);

            Resource resource = new UrlResource(videoPath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.valueOf("video/mp4"))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + fileName + "\"")
                        .body(resource);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/signs")
    public ResponseEntity<List<String>> getAvailableSigns() {
        try {
            Path archivePath = Paths.get(DATASET_PATH);
            if (Files.exists(archivePath)) {
                List<String> signs = Files.list(archivePath)
                        .filter(Files::isDirectory)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
                return ResponseEntity.ok(signs);
            }
            return ResponseEntity.ok(Arrays.asList());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/videos/{signName}")
    public ResponseEntity<List<String>> getVideosForSign(@PathVariable String signName) {
        try {
            Path signPath = Paths.get(DATASET_PATH).resolve(signName);
            if (Files.exists(signPath)) {
                List<String> videos = Files.list(signPath)
                        .filter(path -> path.toString().toLowerCase().endsWith(".mp4"))
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
                return ResponseEntity.ok(videos);
            }
            return ResponseEntity.ok(Arrays.asList());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
