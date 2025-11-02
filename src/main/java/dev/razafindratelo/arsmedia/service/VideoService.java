package dev.razafindratelo.arsmedia.service;

import static java.lang.String.format;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.ProcessSuccessResponse;
import dev.razafindratelo.arsmedia.event.model.EventProducer;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@AllArgsConstructor
@Slf4j
public class VideoService {
  private final VideoRepository repository;
  private final UserService userService;
  private final EventProducer<VideoCompressionRequested> eventProducer;

  public ProcessSuccessResponse compress(
      @NotBlank @Email @NotNull String email, @NotBlank @NotNull String bucketKey) {
    requestCompression(email, bucketKey);
    return new ProcessSuccessResponse(
        "COMPRESSION SUCCESS WITH bucket_key=" + bucketKey,
        "/api/media/videos/compress/" + email + "?bucket_key=" + bucketKey);
  }

  public void requestCompression(
      @NotBlank @Email @NotNull String email, @NotBlank @NotNull String bucketKey) {
    var videoInst =
        repository
            .findByBucketKey(bucketKey)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No video instance found with bucket_key = %s", bucketKey)));

    userService.findByEmail(email);

    VideoCompressionRequested event =
        new VideoCompressionRequested(
            videoInst.getId(), bucketKey, email, CompressionOptions.defaults());

    eventProducer.accept(List.of(event));
    log.info("Video compression event sent for video: {}", videoInst.getId());
  }

  public void requestCompressionWithOptions(
      String email, String bucketKey, CompressionOptions options) {
    var videoInst =
        repository
            .findByBucketKey(bucketKey)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No video instance found with bucket_key = %s", bucketKey)));

    VideoCompressionRequested event =
        new VideoCompressionRequested(videoInst.getId(), bucketKey, email, options);

    eventProducer.accept(List.of(event));
    log.info("Video compression event with custom options sent for video: {}", videoInst.getId());
  }
}
