package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import static org.owasp.encoder.Encode.forJava;

import dev.razafindratelo.arsmedia.model.Audio;
import dev.razafindratelo.arsmedia.model.Image;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.service.MediaService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/media")
@Slf4j
public class MediaUploadController {
  private final MediaService mediaService;

  @PostMapping("/videos/upload")
  public ResponseEntity<Video> uploadVideo(
      @RequestParam("file") @NotNull MultipartFile file,
      @RequestParam("userEmail") @Email String userEmail) {

    log.info("Video upload request received for user: {}", forJava(userEmail));

    Video uploadedVideo = mediaService.uploadVideo(file, userEmail);
    return ResponseEntity.ok(uploadedVideo);
  }

  @PostMapping("/audios/upload")
  public ResponseEntity<Audio> uploadAudio(
      @RequestParam("file") MultipartFile file,
      @RequestParam("userEmail") @Email String userEmail) {

    log.info("Audio upload request received for user: {}", forJava(userEmail));
    Audio uploadedAudio = mediaService.uploadAudio(file, userEmail);
    return ResponseEntity.ok(uploadedAudio);
  }

  @PostMapping("/images/upload")
  public ResponseEntity<Image> uploadImage(
      @RequestParam("file") MultipartFile file,
      @RequestParam("userEmail") @Email String userEmail) {

    log.info("Image upload request received for user: {}", forJava(userEmail));
    Image uploadedImage = mediaService.uploadImage(file, userEmail);
    return ResponseEntity.ok(uploadedImage);
  }
}
