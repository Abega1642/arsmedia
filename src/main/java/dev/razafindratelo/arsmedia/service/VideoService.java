package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static java.lang.String.format;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.event.model.EventProducer;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.repository.CompressedVideoRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.JCompressedVideo;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
  private final CompressedVideoRepository compressedVideoRepository;

  public CompressionJobStatusResponse compress(
      @NotBlank @Email @NotNull String email, @NotBlank @NotNull String bucketKey) {
    return requestCompression(email, bucketKey);
  }

  public CompressionJobStatusResponse requestCompression(
      @NotBlank @Email @NotNull String email, @NotBlank @NotNull String bucketKey) {
    var videoInst =
        repository
            .findByBucketKey(bucketKey)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No video instance found with bucket_key = %s", bucketKey)));

    var user = userService.findByEmail(email);

    if (!videoInst.getOwner().getEmail().equals(user.getEmail())) {
      throw new IllegalArgumentException(
          "Can not perform compression because of invalid information");
    }

    JCompressedVideo compressionJob = new JCompressedVideo();
    compressionJob.setId(UUID.randomUUID().toString());
    compressionJob.setParent(videoInst);
    compressionJob.setCreatedAt(LocalDateTime.now());
    compressionJob.setStatus(ProcessStatus.PENDING);
    compressionJob.setAttemptCount(0);

    compressionJob = compressedVideoRepository.save(compressionJob);

    VideoCompressionRequested event =
        new VideoCompressionRequested(
            videoInst.getId(),
            bucketKey,
            user.getEmail(),
            CompressionOptions.defaults(),
            compressionJob.getId());

    eventProducer.accept(List.of(event));
    log.info(
        "Video compression event sent for video: {}, job_id: {}",
        videoInst.getId(),
        compressionJob.getId());

    return new CompressionJobStatusResponse(
        compressionJob.getId(),
        compressionJob.getStatus(),
        compressionJob.getCreatedAt(),
        null,
        null,
        null,
        null,
        compressionJob.getAttemptCount());
  }

  public CompressionJobStatusResponse requestCompressionWithOptions(
      @NotBlank @Email @NotNull String email,
      @NotBlank @NotNull String bucketKey,
      @NotNull CompressionOptions options) {
    var videoInst =
        repository
            .findByBucketKey(bucketKey)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No video instance found with bucket_key = %s", bucketKey)));

    var user = userService.findByEmail(email);

    JCompressedVideo compressionJob = new JCompressedVideo();
    compressionJob.setId(UUID.randomUUID().toString());
    compressionJob.setParent(videoInst);
    compressionJob.setCreatedAt(LocalDateTime.now());
    compressionJob.setStatus(ProcessStatus.PENDING);
    compressionJob.setAttemptCount(0);

    compressionJob = compressedVideoRepository.save(compressionJob);

    VideoCompressionRequested event =
        new VideoCompressionRequested(
            videoInst.getId(), bucketKey, email, options, compressionJob.getId());

    eventProducer.accept(List.of(event));
    log.info(
        "Video compression event with custom options sent for video: {}, job_id: {}",
        videoInst.getId(),
        compressionJob.getId());

    return new CompressionJobStatusResponse(
        compressionJob.getId(),
        compressionJob.getStatus(),
        compressionJob.getCreatedAt(),
        null,
        null,
        null,
        null,
        compressionJob.getAttemptCount());
  }

  /** Get compression job status by job ID */
  public CompressionJobStatusResponse getCompressionStatus(@NotBlank @NotNull String jobId) {
    JCompressedVideo job =
        compressedVideoRepository
            .findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("Compression job not found: " + jobId));

    return new CompressionJobStatusResponse(
        job.getId(),
        job.getStatus(),
        job.getCreatedAt(),
        job.getCompletedAt(),
        job.getCompressedVideo() != null ? job.getCompressedVideo().getId() : null,
        job.getCompressedVideo() != null ? toVideo(job.getCompressedVideo()).getFilePath() : null,
        job.getErrorMessage(),
        job.getAttemptCount());
  }

  /** Get all compression jobs for a specific video */
  public List<CompressionJobStatusResponse> getCompressionJobsByVideoId(
      @NotBlank @NotNull String videoId) {
    List<JCompressedVideo> jobs = compressedVideoRepository.findByParentId(videoId);

    return jobs.stream()
        .map(
            job ->
                new CompressionJobStatusResponse(
                    job.getId(),
                    job.getStatus(),
                    job.getCreatedAt(),
                    job.getCompletedAt(),
                    job.getCompressedVideo() != null ? job.getCompressedVideo().getId() : null,
                    job.getCompressedVideo() != null
                        ? toVideo(job.getCompressedVideo()).getFilePath()
                        : null,
                    job.getErrorMessage(),
                    job.getAttemptCount()))
        .collect(Collectors.toList());
  }
}
