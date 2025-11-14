package dev.razafindratelo.arsmedia.service;

import static dev.razafindratelo.arsmedia.mapper.VideoMapper.toVideo;
import static java.lang.String.format;

import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.CompressionOptions;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.AudioExtractionJobStatusResponse;
import dev.razafindratelo.arsmedia.endpoint.rest.controller.model.job.VideoCompressionJobStatusResponse;
import dev.razafindratelo.arsmedia.event.model.AudioExtractionRequested;
import dev.razafindratelo.arsmedia.event.model.EventProducer;
import dev.razafindratelo.arsmedia.event.model.VideoCompressionRequested;
import dev.razafindratelo.arsmedia.model.classifier.ProcessStatus;
import dev.razafindratelo.arsmedia.repository.AudioExtractionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoCompressionJobRepository;
import dev.razafindratelo.arsmedia.repository.VideoRepository;
import dev.razafindratelo.arsmedia.repository.model.AudioExtractionJob;
import dev.razafindratelo.arsmedia.repository.model.VideoCompressionJob;
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
  private final EventProducer<VideoCompressionRequested> videoCompressionRequestedEventProducer;
  private final EventProducer<AudioExtractionRequested> audioExtractionRequestedEventProducer;
  private final VideoCompressionJobRepository videoCompressionJobRepository;
  private final AudioExtractionJobRepository audioExtractionJobRepository;

  public VideoCompressionJobStatusResponse compress(
      @NotBlank @Email @NotNull String email, @NotBlank @NotNull String bucketKey) {
    return requestCompression(email, bucketKey);
  }

  public VideoCompressionJobStatusResponse requestCompression(
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

    var compressionJob = new VideoCompressionJob();
    compressionJob.setId(UUID.randomUUID().toString());
    compressionJob.setParent(videoInst);
    compressionJob.setCreatedAt(LocalDateTime.now());
    compressionJob.setStatus(ProcessStatus.PENDING);
    compressionJob.setAttemptCount(0);

    compressionJob = videoCompressionJobRepository.save(compressionJob);

    var event =
        new VideoCompressionRequested(
            videoInst.getId(),
            bucketKey,
            user.getEmail(),
            CompressionOptions.defaults(),
            compressionJob.getId());

    videoCompressionRequestedEventProducer.accept(List.of(event));
    log.info(
        "Video compression event sent for video: {}, job_id: {}",
        videoInst.getId(),
        compressionJob.getId());

    return new VideoCompressionJobStatusResponse(
        compressionJob.getId(),
        compressionJob.getStatus(),
        compressionJob.getCreatedAt(),
        null,
        null,
        null,
        null,
        compressionJob.getAttemptCount());
  }

  public VideoCompressionJobStatusResponse requestCompressionWithOptions(
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

    if (!videoInst.getOwner().getEmail().equals(user.getEmail())) {
      throw new IllegalArgumentException(
          "Can not perform compression because of invalid information");
    }

    VideoCompressionJob compressionJob = new VideoCompressionJob();
    compressionJob.setId(UUID.randomUUID().toString());
    compressionJob.setParent(videoInst);
    compressionJob.setCreatedAt(LocalDateTime.now());
    compressionJob.setStatus(ProcessStatus.PENDING);
    compressionJob.setAttemptCount(0);

    compressionJob = videoCompressionJobRepository.save(compressionJob);

    VideoCompressionRequested event =
        new VideoCompressionRequested(
            videoInst.getId(), bucketKey, email, options, compressionJob.getId());

    videoCompressionRequestedEventProducer.accept(List.of(event));
    log.info(
        "Video compression event with custom options sent for video: {}, job_id: {}",
        videoInst.getId(),
        compressionJob.getId());

    return new VideoCompressionJobStatusResponse(
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
  public VideoCompressionJobStatusResponse getCompressionStatus(@NotBlank @NotNull String jobId) {
    VideoCompressionJob job =
        videoCompressionJobRepository
            .findById(jobId)
            .orElseThrow(() -> new EntityNotFoundException("Compression job not found: " + jobId));

    return new VideoCompressionJobStatusResponse(
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
  public List<VideoCompressionJobStatusResponse> getCompressionJobsByVideoId(
      @NotBlank @NotNull String videoId) {
    List<VideoCompressionJob> jobs = videoCompressionJobRepository.findByParentId(videoId);

    return jobs.stream()
        .map(
            job ->
                new VideoCompressionJobStatusResponse(
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

  public AudioExtractionJobStatusResponse extractAudio(
      @Email @NotBlank String userEmail, @NotNull @NotBlank String bucketKey) {
    var videoInst =
        repository
            .findByBucketKey(bucketKey)
            .orElseThrow(
                () ->
                    new EntityNotFoundException(
                        format("No video instance found with bucket_key = %s", bucketKey)));

    var user = userService.findByEmail(userEmail);

    if (!videoInst.getOwner().getEmail().equals(user.getEmail())) {
      throw new IllegalArgumentException(
          "Can not perform compression because of invalid information");
    }

    var audioExtractionJob = new AudioExtractionJob();
    audioExtractionJob.setId(UUID.randomUUID().toString());
    audioExtractionJob.setParent(videoInst);
    audioExtractionJob.setCreatedAt(LocalDateTime.now());
    audioExtractionJob.setStatus(ProcessStatus.PENDING);
    audioExtractionJob.setAttemptCount(0);

    audioExtractionJobRepository.save(audioExtractionJob);

    var event =
        new AudioExtractionRequested(
            videoInst.getId(),
            videoInst.getBucketKey(),
            videoInst.getOwner().getEmail(),
            audioExtractionJob.getId());

    audioExtractionRequestedEventProducer.accept(List.of(event));

    log.info(
        "Audio extraction event sent for video: {}, job_id: {}",
        videoInst.getId(),
        audioExtractionJob.getId());

    return new AudioExtractionJobStatusResponse(
        event.getJobId(),
        audioExtractionJob.getStatus(),
        audioExtractionJob.getCreatedAt(),
        null,
        null,
        null,
        null,
        audioExtractionJob.getAttemptCount());
  }
}
