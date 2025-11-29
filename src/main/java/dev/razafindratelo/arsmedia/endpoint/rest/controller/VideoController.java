package dev.razafindratelo.arsmedia.endpoint.rest.controller;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.AudioExtractionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.model.Video;
import dev.razafindratelo.arsmedia.model.classifier.ContainerFormat;
import dev.razafindratelo.arsmedia.service.CompressionVideoService;
import dev.razafindratelo.arsmedia.service.VideoService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@AllArgsConstructor
@RequestMapping("/api/media/videos")
public class VideoController {
  private VideoService service;
  private CompressionVideoService compressionService;

  @PostMapping("/compress/{bucketKey:.+}")
  public VideoCompressionJobStatusResponse compress(
      @PathVariable(name = "bucketKey") @NotBlank String bucketKey,
      @RequestParam(name = "userEmail") @NotNull @NotBlank @Email String userEmail) {
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

  @PostMapping("/extract-audio/{bucketKey:.+}")
  public AudioExtractionJobStatusResponse extractAudio(
      @PathVariable(name = "bucketKey") @NotBlank String bucketKey,
      @RequestParam(name = "userEmail") @NotNull @NotBlank @Email String userEmail) {
    return service.extractAudio(userEmail, bucketKey);
  }

  @GetMapping("/audio-extraction-status/{jobId}")
  public AudioExtractionJobStatusResponse getAudioExtractionStatus(
      @PathVariable(name = "jobId") @NotBlank String jobId) {
    return service.getAudioExtractionStatus(jobId);
  }

  @PostMapping("/convert/{bucketKey:.+}")
  public Video convertVideo(
      @PathVariable("bucketKey") @NotNull @NotBlank String bucketKey,
      @RequestParam("to") @NotNull ContainerFormat toFormat,
      @RequestParam("from") @Email @NotNull @NotBlank String userEmail) {
    return service.convertTo(toFormat, bucketKey, userEmail);
  }
}
