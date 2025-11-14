package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.service.CompressionVideoService;
import dev.razafindratelo.arsmedia.service.VideoService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@AllArgsConstructor
@RequestMapping("/api/media/videos")
public class VideoController {
  private VideoService service;
  private CompressionVideoService compressionService;

  @PostMapping("/compress/{userEmail}")
  public CompressionJobStatusResponse compress(
      @PathVariable(name = "userEmail") @Email @NotBlank String userEmail,
      @RequestParam(name = "bucket_key") @NotNull @NotBlank String bucketKey) {
    return service.compress(userEmail, bucketKey);
  }

  @GetMapping("/compressed")
  public List<Video> getAllCompressedVideosOfUser(
      @RequestParam(name = "userEmail") @Email @NotBlank String userEmail) {
    return compressionService.getAllCompressedVideos(userEmail);
  }

  @GetMapping("/compression-status/{jobId}")
  public CompressionJobStatusResponse getCompressionStatus(
      @PathVariable(name = "jobId") @NotBlank String jobId) {
    return service.getCompressionStatus(jobId);
  }

  @GetMapping("/compression-jobs/{videoId}")
  public List<CompressionJobStatusResponse> getCompressionJobsByVideoId(
      @PathVariable(name = "videoId") @NotBlank String videoId) {
    return service.getCompressionJobsByVideoId(videoId);
  }
}
