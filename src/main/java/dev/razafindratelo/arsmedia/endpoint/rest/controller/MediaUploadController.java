package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.service.MediaService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@AllArgsConstructor
@RequestMapping("/api/media")
public class MediaUploadController {
  private final MediaService service;

  public static File convertToFile(@NotNull @NotBlank @NotEmpty MultipartFile multipartFile)
      throws IOException {
    String originalName = multipartFile.getOriginalFilename();
    String suffix =
        (originalName != null && originalName.contains("."))
            ? originalName.substring(originalName.lastIndexOf('.'))
            : ".tmp";

    File tempFile = File.createTempFile("upload-", suffix);

    multipartFile.transferTo(tempFile);
    tempFile.deleteOnExit();

    return tempFile;
  }

  @PostMapping("/videos/upload")
  public Video uploadVideo(
      @RequestParam("file") MultipartFile file, @RequestParam("userEmail") String userEmail)
      throws IOException {
    var videoFile = convertToFile(file);
    return service.uploadVideo(videoFile, userEmail);
  }
}
