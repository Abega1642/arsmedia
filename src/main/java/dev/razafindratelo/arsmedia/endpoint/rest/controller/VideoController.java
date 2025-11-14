package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.AudioExtractionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
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
  public VideoCompressionJobStatusResponse compress(
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
  public VideoCompressionJobStatusResponse getCompressionStatus(
      @PathVariable(name = "jobId") @NotBlank String jobId) {
    return service.getCompressionStatus(jobId);
  }

  @GetMapping("/compression-jobs/{videoId}")
  public List<VideoCompressionJobStatusResponse> getCompressionJobsByVideoId(
      @PathVariable(name = "videoId") @NotBlank String videoId) {
    return service.getCompressionJobsByVideoId(videoId);
  }

  @PostMapping("/extract-audio/{userEmail}")
  public AudioExtractionJobStatusResponse extractAudio(
      @PathVariable(name = "userEmail") @Email @NotBlank String userEmail,
      @RequestParam(name = "bucket_key") @NotNull @NotBlank String bucketKey) {
    return service.extractAudio(userEmail, bucketKey);
  }
}
